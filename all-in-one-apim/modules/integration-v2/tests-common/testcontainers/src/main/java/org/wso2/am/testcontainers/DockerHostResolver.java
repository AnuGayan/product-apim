/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DockerHostResolver {

    private static final Logger log = LoggerFactory.getLogger(DockerHostResolver.class);
    private static final String DOCKER_HOST_INTERNAL = "host.docker.internal";

    private static String colimaHostIp = null;
    private static String colimaDockerSocketPath = null;
    private static boolean isColimaEnvironment;

    static {
        isColimaEnvironment = System.getenv("COLIMA_VERSION") != null;
        if (isColimaEnvironment) {
            resolveColimaConfiguration();
        }
    }

    private static void resolveColimaConfiguration() {
        try {
            Process process = Runtime.getRuntime().exec("colima ls -j");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            ObjectMapper objectMapper = new ObjectMapper();

            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    // We only need one valid configuration, so we take the first one that is running.
                    if (jsonNode.has("running") && jsonNode.get("running").asBoolean()) {
                        if (jsonNode.has("address")) {
                            colimaHostIp = jsonNode.get("address").asText();
                        }
                        if (jsonNode.has("docker")) {
                            colimaDockerSocketPath = jsonNode.get("docker").asText();
                        }
                        // Break after finding the first running instance.
                        break;
                    }
                } catch (IOException e) {
                    log.warn("Could not parse JSON from colima output line: {}", line, e);
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error while getting Colima configuration", e);
        }

        if (colimaHostIp == null || colimaDockerSocketPath == null) {
            log.warn("Could not determine Colima host IP or Docker socket path. Testcontainers might not work correctly.");
        } else {
            log.info("Detected Colima environment. Host IP: {}, Docker Socket: {}", colimaHostIp, colimaDockerSocketPath);
        }
    }

    /**
     * Get the docker host ip, considering Colima.
     *
     * @return docker host ip
     */
    public static String getDockerHost() {
        if (isColimaEnvironment && colimaHostIp != null) {
            return colimaHostIp;
        }
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * Configure Testcontainers to use the correct Docker socket and host ip for Colima.
     */
    public static void configureTestcontainers() {
        if (isColimaEnvironment) {
            if (colimaDockerSocketPath != null) {
                System.setProperty("docker.host", "unix://" + colimaDockerSocketPath);
                log.info("Set system property 'docker.host' to: unix://{}", colimaDockerSocketPath);
            }
            if (colimaHostIp != null) {
                System.setProperty("testcontainers.host.override", colimaHostIp);
                log.info("Set system property 'testcontainers.host.override' to: {}", colimaHostIp);
            }
        }
    }
}
