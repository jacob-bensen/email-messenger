package com.emailmessenger.email;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 ExecutionCondition that disables a test class when Docker is not available.
 * Must be registered before @Testcontainers so it fires first, preventing container startup.
 */
class RequiresDocker implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
        } catch (Exception ignored) {
        }
        return ConditionEvaluationResult.disabled(
                "Docker daemon not available — skipping Testcontainers PostgreSQL integration tests");
    }
}
