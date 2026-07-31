package com.jeonghuny.ext_blocker.policy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/extensions")
public class ExtensionPolicyController {

    private final ExtensionPolicyService service;

    public ExtensionPolicyController(ExtensionPolicyService service) {
        this.service = service;
    }

    public record ExtensionView(String name, boolean blocked) {
        static ExtensionView of(BlockedExtension e) {
            return new ExtensionView(e.getName(), e.isBlocked());
        }
    }

    @GetMapping
    public Map<String, Object> list() {
        List<ExtensionView> fixed  = service.findFixed().stream().map(ExtensionView::of).toList();
        List<ExtensionView> custom = service.findCustom().stream().map(ExtensionView::of).toList();
        return Map.of(
                "fixed", fixed,
                "custom", custom,
                "customCount", custom.size(),
                "customLimit", ExtensionPolicyService.MAX_CUSTOM,
                "maxNameLength", ExtensionPolicyService.MAX_NAME_LENGTH
        );
    }

    public record ToggleRequest(boolean blocked) { }

    @PatchMapping("/fixed/{name}")
    public ResponseEntity<Void> toggleFixed(@PathVariable String name,
                                            @RequestBody ToggleRequest req) {
        service.toggleFixed(name, req.blocked());
        return ResponseEntity.noContent().build();
    }

    public record AddRequest(String name) { }

    @PostMapping("/custom")
    public ResponseEntity<ExtensionView> addCustom(@RequestBody AddRequest req) {
        BlockedExtension saved = service.addCustom(req.name());
        return ResponseEntity.status(201).body(ExtensionView.of(saved));
    }

    @DeleteMapping("/custom/{name}")
    public ResponseEntity<Void> deleteCustom(@PathVariable String name) {
        service.deleteCustom(name);
        return ResponseEntity.noContent().build();
    }
}