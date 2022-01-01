/*
 * Copyright 2021 Coveo Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.coveo.tasks

import com.netflix.spinnaker.orca.api.pipeline.RetryableTask
import com.netflix.spinnaker.orca.api.pipeline.TaskResult
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.KatoService
import com.netflix.spinnaker.orca.clouddriver.utils.CloudProviderAware
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import com.netflix.spinnaker.orca.coveo.tasks.job.NestedKubernetesJobRunner
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RunInnerJobTask(
  private val kato: KatoService,
  private val nestedKubernetesJobRunner: NestedKubernetesJobRunner
) : CloudProviderAware, InnerJobAware, RetryableTask {
  override fun execute(stage: StageExecution): TaskResult {
    val credentials = getCredentials(stage)
    val cloudProvider = getCloudProvider(stage)
    if (cloudProvider != "kubernetes") {
      throw IllegalStateException(
        "Invalid CloudProvider $cloudProvider. Only Kubernetes is currently supported"
      )
    }

    val operations = nestedKubernetesJobRunner.getOperations(stage)
    val taskId = kato.requestOperations(cloudProvider, operations)

    val jobContext: MutableMap<String, Any?> =
      mutableMapOf(
        "deploy.account.name" to credentials,
        "notification.type" to "runjob",
        "kato.result.expected" to nestedKubernetesJobRunner.isKatoResultExpected,
        "kato.last.task.id" to taskId
      )
    jobContext.putAll(nestedKubernetesJobRunner.getAdditionalOutputs(stage, operations))

    return TaskResult.builder(ExecutionStatus.SUCCEEDED)
      .context(jobContext + getUpdatedContextWithInnerJob(stage, jobContext))
      .build()
  }

  override fun getBackoffPeriod(): Long = TimeUnit.SECONDS.toMillis(2)
  override fun getTimeout(): Long = TimeUnit.SECONDS.toMillis(60)
}
