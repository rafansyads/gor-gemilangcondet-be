package io.mpruy.gor_gemilangcondet.backend_api.controller;

import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseRequestDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.BaseResponseDto;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.CreateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.requests.UpdateLapanganRequest;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganResponse;
import io.mpruy.gor_gemilangcondet.backend_api.dto.reservations.responses.LapanganLogResponse;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.reservations.LapanganType;
import io.mpruy.gor_gemilangcondet.backend_api.service.ReservasiService;
import io.mpruy.gor_gemilangcondet.backend_api.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class LapanganController {

    private final ReservasiService reservasiService;

    // GET /courts
    @GetMapping
    public ResponseEntity<BaseResponseDto<List<LapanganResponse>>> getCourts(
            @RequestParam(required = false) LapanganType type) {
        List<LapanganResponse> courts = (type != null)
                ? reservasiService.getCourtsByType(type)
                : reservasiService.getAllCourts();
        return ResponseUtil.success(courts, "Data lapangan berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // POST /courts
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @PostMapping
    public ResponseEntity<BaseResponseDto<LapanganResponse>> createCourt(
            @Validated @RequestBody BaseRequestDto<CreateLapanganRequest> request) {
        LapanganResponse response = reservasiService.createCourt(request.getData());
        return ResponseUtil.success(response, "Lapangan berhasil ditambahkan", HttpStatus.CREATED)
                .toBuilder().build();
    }

    // GET /courts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDto<LapanganResponse>> getCourt(@PathVariable UUID id) {
        LapanganResponse response = reservasiService.getCourt(id);
        return ResponseUtil.success(response, "Data lapangan berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }

    // POST /courts/{id}/image — Upload court image
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDto<LapanganResponse>> uploadCourtImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        LapanganResponse response = reservasiService.uploadCourtImage(id, file);
        return ResponseUtil.success(response, "Gambar lapangan berhasil diunggah", HttpStatus.OK)
                .toBuilder().build();
    }

    // GET /courts/image/{filename} — Serve court image
    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getCourtImage(@PathVariable String filename) throws Exception {
        Path filePath = Paths.get("uploads/court-images").resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) contentType = "image/png";
        else if (lower.endsWith(".webp")) contentType = "image/webp";
        else if (lower.endsWith(".gif")) contentType = "image/gif";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // PUT /courts/{id}
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDto<LapanganResponse>> updateCourt(
            @PathVariable UUID id,
            @Validated @RequestBody BaseRequestDto<UpdateLapanganRequest> request) {
        LapanganResponse response = reservasiService.updateCourt(id, request.getData());
        return ResponseUtil.success(response, "Lapangan berhasil diperbarui", HttpStatus.OK)
                .toBuilder().build();
    }

    // DELETE /courts/{id}
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDto<Object>> deleteCourt(@PathVariable UUID id) {
        reservasiService.deleteCourt(id);
        return ResponseUtil.success(null, "Lapangan berhasil dihapus", HttpStatus.OK)
                .toBuilder().build();
    }

    // PATCH /courts/{id}/status
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponseDto<LapanganResponse>> updateCourtStatus(
            @PathVariable UUID id,
            @RequestParam LapanganStatus status) {
        LapanganResponse response = reservasiService.updateCourtStatus(id, status);
        return ResponseUtil.success(response, "Status lapangan berhasil diperbarui", HttpStatus.OK)
                .toBuilder().build();
    }

    // GET /courts/{id}/logs
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAF_LAPANGAN')")
    @GetMapping("/{id}/logs")
    public ResponseEntity<BaseResponseDto<List<LapanganLogResponse>>> getCourtLogs(@PathVariable UUID id) {
        List<LapanganLogResponse> logs = reservasiService.getCourtLogs(id);
        return ResponseUtil.success(logs, "Riwayat perubahan lapangan berhasil diambil", HttpStatus.OK)
                .toBuilder().build();
    }
}
