package com.jeonghuny.ext_blocker.upload;

import com.jeonghuny.ext_blocker.policy.ExtensionPolicyService;
import com.jeonghuny.ext_blocker.validation.ExtensionPolicyValidator;
import com.jeonghuny.ext_blocker.validation.PolicyDecision;
import com.jeonghuny.ext_blocker.validation.RejectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private final ExtensionPolicyService policyService;
    private final ExtensionPolicyValidator validator = new ExtensionPolicyValidator();
    private final FileSignatureInspector inspector = new FileSignatureInspector();

    public UploadController(ExtensionPolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file)
            throws IOException {

        String original = file.getOriginalFilename();

        if (file.isEmpty()) {
            return reject("EMPTY_FILE", "빈 파일은 업로드할 수 없습니다.", original, null);
        }

        // 1~2. 파일명 + 확장자 정책
        Set<String> blocked = policyService.findBlockedNames();
        PolicyDecision decision = validator.validate(original, blocked);

        if (!decision.allowed()) {
            log.warn("업로드 거부 | reason={} | matched={} | name={}",
                    decision.reason(), decision.matchedExtension(), sanitizeForLog(original));
            return reject(
                    decision.reason().name(),
                    buildMessage(decision),
                    original,
                    decision.matchedExtension());
        }

        // 3. 시그니처 — 확장자가 통과해도 내용이 실행 파일이면 거부
        try (InputStream in = file.getInputStream()) {
            String exeType = inspector.detectExecutable(in);
            if (exeType != null) {
                log.warn("업로드 거부 | reason=EXECUTABLE_CONTENT | type={} | name={}",
                        exeType, sanitizeForLog(original));
                return reject("EXECUTABLE_CONTENT",
                        "확장자와 무관하게 실행 파일로 판별되어 차단했습니다. (" + exeType + ")",
                        original, null);
            }
        }

        // 4. 통과.
        //    실측 결과 컨테이너 파일시스템은 휘발성이며,
        //    공개 배포 환경에 업로드 파일을 보관하지 않기 위해 저장하지 않는다.
        log.info("업로드 허용 | size={} | name={}", file.getSize(), sanitizeForLog(original));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", "ACCEPTED");
        body.put("originalFilename", original);
        body.put("size", file.getSize());
        body.put("message", "업로드가 허용되었습니다.");
        return ResponseEntity.ok(body);
    }

    private String buildMessage(PolicyDecision d) {
        if (d.reason() == RejectReason.BLOCKED_EXTENSION) {
            return "차단된 확장자입니다. (." + d.matchedExtension() + ")";
        }
        return d.reason().message();
    }

    private ResponseEntity<Map<String, Object>> reject(
            String error, String message, String name, String matched) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("result", "REJECTED");
        body.put("error", error);
        body.put("message", message);
        body.put("originalFilename", name);
        if (matched != null) body.put("matchedExtension", matched);
        return ResponseEntity.badRequest().body(body);
    }

    /** 로그 인젝션 방지 — 개행/제어문자로 로그를 위조하지 못하게 함 */
    private String sanitizeForLog(String s) {
        if (s == null) return "(null)";
        String t = s.replaceAll("[\\p{Cc}\\p{Cf}]", "?");
        return t.length() > 120 ? t.substring(0, 120) + "…" : t;
    }
}