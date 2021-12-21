/*
 * Copyright 2021 Coveo Solutions inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
 *
 */

package com.netflix.spinnaker.orca.coveo.tasks

import com.netflix.spinnaker.orca.api.pipeline.Task
import com.netflix.spinnaker.orca.api.pipeline.TaskResult
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.coveo.ext.skipStage
import com.netflix.spinnaker.orca.coveo.ext.skipStageSinceNoChange
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import org.springframework.stereotype.Component

@Component
class PrepareTerraformApplyTask : InnerJobAware, Task {
  override fun execute(stage: StageExecution): TaskResult {
    val context = stage.context

    val savedJobDetailsBeforeClear =
      mapOf(
        "deploy.jobs" to context.remove("deploy.jobs"),
        "completionDetails" to context.remove("completionDetails"),
        "jobStatus" to context.remove("jobStatus"),
        "kato.task.terminalRetryCount" to context.remove("kato.task.terminalRetryCount"),
        "kato.task.firstNotFoundRetry" to context.remove("kato.task.firstNotFoundRetry"),
        "kato.last.task.id" to context.remove("kato.last.task.id"),
        "kato.task.notFoundRetryCount" to context.remove("kato.task.notFoundRetryCount"),
        "kato.task.lastStatus" to context.remove("kato.task.lastStatus"),
        "kato.result.expected" to context.remove("kato.result.expected"),
        "kato.tasks" to context.remove("kato.tasks"),
        "propertyFileContents" to context.remove("propertyFileContents"),
      )

    // TODO: Add proper check
    if (true) {
      stage.skipStageSinceNoChange()
    }

    return TaskResult.builder(ExecutionStatus.SUCCEEDED)
      .context(
        mapOf("currentInnerJob" to "terraformApply") +
          getUpdatedInnerJobContext(
            stage,
            mapOf("savedJobDetailsBeforeClear" to savedJobDetailsBeforeClear)
          )
      )
      .build()
  }
}
