package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {

    // GET /urls?page=value
    public static Handler showAllUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

//        int totalRowsCount = pagedUrls.getTotalCount();
//        int lastPageNumber = (int) Math.ceil((double) totalRowsCount / rowsPerPage);
//        List<Integer> pageNumbers = IntStream.rangeClosed(1, lastPageNumber).boxed().toList();

        ctx.attribute("currentPage", currentPage);
        ctx.attribute("pages", pages);
        ctx.attribute("urls", urls);
        ctx.render("urls/index.html");
    };

    // GET /urls/{id}
    public static Handler showUrl = ctx -> {
        long urlId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
            .id.equalTo(urlId)
            .findOne();

        if (url == null) {
            ctx.sessionAttribute("flash", "Страница не найдена");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        if (url.getUrlChecks().size() > 1) {
            url.getUrlChecks().sort(Comparator.comparing(UrlCheck::getCreatedAt).reversed());
        }
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    // POST /urls add website
    public static Handler createUrl = ctx -> {
        String urlFull = ctx.formParam("url");
        String urlConstructed = null;

        try {
            URL url = new URL(urlFull);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int portNumber = url.getPort(); // if URL doesn't specify a port number, return -1
            String port = (portNumber == -1) ? "" : ":" + portNumber;
            urlConstructed = protocol + "://" + host + port;
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.sessionAttribute("incorrectUrl", urlFull);
            ctx.redirect("/");
            return;
        }

        Url urlPresent = new QUrl()
                .name.equalTo(urlConstructed)
                .findOne();

        if (urlPresent == null) {
            Url urlNew = new Url(urlConstructed);
            urlNew.save();
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "info");
        } else {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls");
    };

    public static Handler createUrlCheck = ctx -> {
        long urlId = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url urlPresent = new QUrl()
                .id.equalTo(urlId)
                .findOne();

        try {
            HttpResponse<String> response = Unirest.get(urlPresent.getName()).asString();
            Document document = Jsoup.parse(response.getBody());

            int httpStatusCode = response.getStatus();
            String title = document.title();
            String h1 = getH1(document);
            String description = getDescription(document);

            UrlCheck check = new UrlCheck(httpStatusCode, title, h1, description, urlPresent);
            check.save();
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + urlId);
    };

    @Nullable
    private static String getH1(Document document) {
        Element h1 = document.selectFirst("h1");
        String h1String = h1.text();
        return h1String.isEmpty() ? null : h1String;
    }

    @Nullable
    private static String getDescription(Document document) {
        Element description = document.selectFirst("meta[name='description']");
        String descriptionString = description.attr("content");
        return descriptionString.isEmpty() ? null : descriptionString;
    }
}
