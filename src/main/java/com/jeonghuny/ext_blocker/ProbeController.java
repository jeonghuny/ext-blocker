package com.jeonghuny.ext_blocker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/probe")
public class ProbeController {

    private static final Path TMP = Path.of(System.getProperty("java.io.tmpdir"), "probe");
    private final JdbcTemplate jdbc;
    private final Instant startedAt = Instant.now();

    public ProbeController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws IOException {
        Files.createDirectories(TMP);
        Path saved = TMP.resolve(UUID.randomUUID() + ".bin");
        file.transferTo(saved);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("originalFilename", file.getOriginalFilename());
        r.put("declaredContentType", file.getContentType());  // ← 클라이언트가 "주장"한 값
        r.put("size", file.getSize());
        r.put("savedPath", saved.toString());
        return r;
    }

    @GetMapping("/status")
    public Map<String, Object> status() throws IOException {
        List<String> tmpFiles = List.of();
        if (Files.exists(TMP)) {
            try (var s = Files.list(TMP)) {
                tmpFiles = s.map(p -> p.getFileName().toString()).toList();
            }
        }
        long t0 = System.nanoTime();
        String dbNow = jdbc.queryForObject("select now()::text", String.class);
        long dbMs = (System.nanoTime() - t0) / 1_000_000;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds());
        r.put("tmpFileCount", tmpFiles.size());
        r.put("tmpFiles", tmpFiles);
        r.put("dbNow", dbNow);
        r.put("dbLatencyMs", dbMs);
        return r;
    }
}
