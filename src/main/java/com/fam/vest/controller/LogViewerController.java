package com.fam.vest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/rest/v1/logs")
@CrossOrigin
public class LogViewerController {


    @Value("${fam.vest.app.log.viewer.lines.limit}")
    private int logViewerLinesLimit;

    @GetMapping(value = "/{logFile}", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public String getApplicationLogs(@PathVariable String logFile) throws IOException {
        Path logFilePath = Paths.get("logs/" + logFile);
        return Files.readAllLines(logFilePath)
                .stream()
                .skip(Math.max(0, Files.readAllLines(logFilePath).size() - logViewerLinesLimit))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @GetMapping(value = "/algo/{logFile}", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public String getAlgoLogs(@PathVariable String logFile) throws IOException {
        Path logFilePath = Paths.get("logs/algo/" + logFile);
        return String.join("\n", Files.readAllLines(logFilePath));
    }
}
