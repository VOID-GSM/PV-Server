package pv.domain.imports.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import pv.global.exception.CustomException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Component
public class NotionZipParser {

    private static final int MAX_ZIP_ENTRIES = 10_000;
    private static final long MAX_DOCUMENT_BYTES = 2L * 1024 * 1024;
    private static final int MAX_PATH_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Pattern NOTION_ID_SUFFIX = Pattern.compile("\\s[0-9a-fA-F]{32}$");
    private static final Pattern DRIVE_PREFIX = Pattern.compile("^[A-Za-z]:.*");

    private final int maxDocuments;
    private final long maxTotalBytes;

    public NotionZipParser(@Value("${import.max-entries}") int maxDocuments,
                           @Value("${import.max-uncompressed-size}") DataSize maxTotalSize) {
        this.maxDocuments = maxDocuments;
        this.maxTotalBytes = maxTotalSize.toBytes();
    }

    public record ParsedDocument(String sourcePath, String title, String content) {
    }

    public List<ParsedDocument> parse(Path zipPath) {
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            if (zip.size() > MAX_ZIP_ENTRIES) {
                throw bad("ZIP 안의 파일이 너무 많습니다.");
            }

            List<ParsedDocument> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            long total = 0;

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // 모든 엔트리의 경로를 먼저 검사 (Zip Slip)
                String path = safePath(entry.getName());
                if (entry.isDirectory() || !isMarkdown(path) || isMacMetadata(path)) {
                    continue;
                }
                if (path.length() > MAX_PATH_LENGTH) {
                    throw bad("파일 경로가 너무 깁니다.");
                }
                if (!seen.add(path)) {
                    continue;
                }
                if (result.size() >= maxDocuments) {
                    throw bad("문서가 너무 많습니다. 최대 " + maxDocuments + "개까지 가져올 수 있습니다.");
                }

                long limit = Math.min(MAX_DOCUMENT_BYTES, maxTotalBytes - total);
                byte[] bytes = readLimited(zip, entry, limit);
                total += bytes.length;

                result.add(toDocument(path, bytes));
            }

            if (result.isEmpty()) {
                throw bad("가져올 마크다운 문서가 없습니다.");
            }
            return result;
        } catch (ZipException e) {
            throw bad("올바른 ZIP 파일이 아닙니다.");
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "ZIP 파일을 읽는 중 오류가 발생했습니다.");
        }
    }

    // 경로 검증: 절대경로, 드라이브 문자, ".." 구간이 있으면 거절
    private String safePath(String rawName) {
        String name = rawName.replace('\\', '/');
        if (name.isEmpty() || name.indexOf('\0') >= 0
                || name.startsWith("/") || DRIVE_PREFIX.matcher(name).matches()) {
            throw bad("허용되지 않는 파일 경로가 포함되어 있습니다.");
        }
        for (String segment : name.split("/")) {
            if (segment.equals("..")) {
                throw bad("허용되지 않는 파일 경로가 포함되어 있습니다.");
            }
        }
        return name;
    }

    private boolean isMarkdown(String path) {
        return path.toLowerCase(Locale.ROOT).endsWith(".md");
    }

    private boolean isMacMetadata(String path) {
        return path.startsWith("__MACOSX/") || fileName(path).startsWith("._");
    }

    // 헤더에 적힌 크기는 믿지 않고, 실제로 읽은 바이트 수로 제한
    private byte[] readLimited(ZipFile zip, ZipEntry entry, long limit) throws IOException {
        if (limit <= 0 || (entry.getSize() >= 0 && entry.getSize() > limit)) {
            throw bad("압축 해제 크기 제한을 초과했습니다.");
        }
        try (InputStream in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            long written = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                written += n;
                if (written > limit) {
                    throw bad("압축 해제 크기 제한을 초과했습니다.");
                }
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private ParsedDocument toDocument(String path, byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        String title = null;
        String body = text;

        int firstLineEnd = text.indexOf('\n');
        String firstLine = (firstLineEnd >= 0 ? text.substring(0, firstLineEnd) : text).strip();
        if (firstLine.startsWith("# ")) {
            title = firstLine.substring(2).strip();
            body = firstLineEnd >= 0 ? text.substring(firstLineEnd + 1).stripLeading() : "";
        }

        if (title == null || title.isEmpty()) {
            title = titleFromFileName(path);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }
        return new ParsedDocument(path, title, body);
    }

    private String titleFromFileName(String path) {
        String name = fileName(path);
        name = name.substring(0, name.length() - 3); // ".md" 제거
        name = NOTION_ID_SUFFIX.matcher(name).replaceFirst("").strip();
        return name.isEmpty() ? "제목 없음" : name;
    }

    private String fileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private CustomException bad(String message) {
        return new CustomException(HttpStatus.BAD_REQUEST, message);
    }
}