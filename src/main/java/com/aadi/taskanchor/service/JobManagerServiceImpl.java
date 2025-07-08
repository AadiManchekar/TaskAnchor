package com.aadi.taskanchor.service;

import com.aadi.jobmanager.CreateJobRequest;
import com.aadi.jobmanager.CreateJobResponse;
import com.aadi.jobmanager.JobManagerGrpc.JobManagerImplBase;

import io.grpc.stub.StreamObserver;

import lombok.extern.slf4j.Slf4j;

import org.springframework.grpc.server.service.GrpcService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@GrpcService
public class JobManagerServiceImpl extends JobManagerImplBase {

    @Override
    public void createJob(
            CreateJobRequest request, StreamObserver<CreateJobResponse> responseObserver) {
        log.info("Received request to create job with command: {}", request.getCommand());

        try {
            String command = request.getCommand();
            List<String> commandList = Arrays.asList("bash", "-c", command); // For Unix
            ProcessBuilder builder = new ProcessBuilder(commandList);

            Process process = builder.start();
            long jobId = process.pid();

            // Optional: wait for process to finish with timeout
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            // Collect stdout and stderr
            String output =
                    new BufferedReader(new InputStreamReader(process.getInputStream()))
                            .lines()
                            .reduce("", (acc, line) -> acc + line + "\n");

            String error =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()))
                            .lines()
                            .reduce("", (acc, line) -> acc + line + "\n");

            int exitCode = process.exitValue();

            CreateJobResponse response =
                    CreateJobResponse.newBuilder()
                            .setJobId(String.valueOf(jobId))
                            .setExitCode(exitCode)
                            .setErrorMessage(error)
                            .setOutput(output)
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Job creation response sent: {}", response);

        } catch (Exception e) {
            log.error("Error creating job", e);
        }
    }
}
