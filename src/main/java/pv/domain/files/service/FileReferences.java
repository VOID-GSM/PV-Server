package pv.domain.files.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileReferences {
    private static final Pattern FILE_REF = Pattern.compile("\\(file:(\\d+)\\)");

    private FileReferences() {
    }

    public static Set<Long> extractFileIds(String markdown) {
        Set<Long> ids = new LinkedHashSet<>();
        if (markdown == null || markdown.isEmpty()) {
            return ids;
        }
        Matcher matcher = FILE_REF.matcher(markdown);
        while (matcher.find()) {
            ids.add(Long.parseLong(matcher.group(1)));
        }
        return ids;
    }
}
