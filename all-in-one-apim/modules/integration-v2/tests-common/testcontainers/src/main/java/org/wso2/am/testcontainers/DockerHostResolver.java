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

    /**
     * Get the docker host ip.
     *
     * @return docker host ip
     */
    public static String getDockerHost() {

        String dockerHost = DockerClientFactory.instance().dockerHostIpAddress();
        if (isColima()) {
            dockerHost = getColimaHostIp();
        }
        return dockerHost;
    }

    /**
     * Check if the docker host is Colima.
     *
     * @return true if the docker host is Colima, false otherwise
     */
    private static boolean isColima() {

        String colimaVersion = System.getenv("COLIMA_VERSION");
        return colimaVersion != null && !colimaVersion.isEmpty();
    }

    /**
     * Get the Colima host ip.
     *
     * @return Colima host ip
     */
    private static String getColimaHostIp() {

        try {
            Process process = Runtime.getRuntime().exec("colima ls -j");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            ObjectMapper objectMapper = new ObjectMapper();

            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    if (jsonNode.has("address")) {
                        String address = jsonNode.get("address").asText();
                        if (address != null && !address.isEmpty()) {
                            log.info("Colima host ip: {}", address);
                            process.waitFor();
                            return address;
                        }
                    }
                } catch (IOException e) {
                    // Ignore malformed JSON lines
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error while getting Colima host ip", e);
        }
        log.warn("Could not determine Colima host ip. Falling back to {}", DOCKER_HOST_INTERNAL);
        return DOCKER_HOST_INTERNAL;
    }

}
