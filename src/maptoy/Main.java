package maptoy;

import spark.ExceptionHandler;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static Set<String> PROXY_PASS_HEADERS = new HashSet<>(Arrays.asList("content-type", "content-length",
            "last-modified", "cache-control"));

    static class BadRequestException extends Exception {
        public BadRequestException(String msg) {
            super(msg);
        }
    }

    static String checkObjId(String objId) throws BadRequestException {
        if (!Pattern.matches("^nla.obj-[0-9]+$", objId)) {
            throw new BadRequestException("invalid object id");
        }
        return objId;
    }

    public static void main(String[] args) {
        Spark.ipAddress(System.getenv().getOrDefault("ADDRESS", "127.0.0.1"));
        Spark.port(Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")));

        Spark.exception(BadRequestException.class, new ExceptionHandler<BadRequestException>() {
            @Override
            public void handle(BadRequestException e, Request request, Response response) {
                response.status(400);
                response.type("text/plain");
                response.body(e.toString());
            }
        });

        Pattern RE_TILE = Pattern.compile("(\\d+)-(\\d+)-(\\d+)\\.jpg");

        Spark.get("/map/:id/zoomify/:tilegroup/:tile", (req, res) -> {
            String objId = checkObjId(req.params(":id"));
            Matcher m = RE_TILE.matcher(req.params(":tile"));
            if (!m.matches()) {
                throw new BadRequestException("invalid tile path, try TileGroup0/0-0-0.jpg");
            }
            int r = Integer.parseInt(m.group(1));
            int x = Integer.parseInt(m.group(2));
            int y = Integer.parseInt(m.group(3));

            res.header("Access-Control-Allow-Origin", "*");

            URL url = new URL("http://nla.gov.au/" + objId + "/dzi?tile=" + (r + 8) + "/" + x + "_" + y + ".jpg");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
                    if (header.getKey() != null && PROXY_PASS_HEADERS.contains(header.getKey().toLowerCase())) {
                        res.header(header.getKey(), header.getValue().get(0));
                    }
                }
                try (InputStream stream = conn.getInputStream();
                     OutputStream out = res.raw().getOutputStream()) {
                    byte[] buf = new byte[8192];
                    for (int n = stream.read(buf); n >= 0; n = stream.read(buf)) {
                        out.write(buf, 0, n);
                    }
                }
            } finally {
                conn.disconnect();
            }
            return "";
        });

        Spark.get("/", (req, res) -> {
            String mapHtml;
            try (InputStream stream = Main.class.getResourceAsStream("map.html")) {
                mapHtml = new Scanner(stream).useDelimiter("\\Z").next();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return mapHtml;
        });

    }
}