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

package com.netflix.spinnaker.orca.coveo.pipeline

import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.pipeline.job.RunJobStage
import com.netflix.spinnaker.orca.clouddriver.pipeline.job.RunJobStageDecorator
import com.netflix.spinnaker.orca.clouddriver.tasks.job.DestroyJobTask
import com.netflix.spinnaker.orca.coveo.tasks.*
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import com.netflix.spinnaker.orca.echo.pipeline.ManualJudgmentStage.WaitForManualJudgmentTask
import com.netflix.spinnaker.orca.ext.withTask
import org.springframework.stereotype.Component

val INNER_KEYS_TO_CLEAR_ON_RESTART =
  setOf("completionDetails", "deploy.jobs", "jobStatus", "propertyFileContents")

@Component
class TerraformStage(
  destroyJobTask: DestroyJobTask,
  runJobStageDecorators: List<RunJobStageDecorator>
) : RunJobStage(destroyJobTask, runJobStageDecorators), InnerJobAware {
  override fun taskGraph(stage: StageExecution, builder: TaskNode.Builder) {
    // Plan
    stage.context["currentInnerJob"] = "terraformPlan"
    appendTerraformStage("terraformPlan", stage, builder)

    // Approval
    builder.withTask<PrepareTerraformApplyTask>("prepareTerraformApply")
    val isAutoApproved =
      stage
        .context
        .getOrDefault("isAutoApproved", false)
        .toString()
        .equals("true", ignoreCase = true)
    if (!isAutoApproved) {
      builder.withTask<WaitForManualJudgmentTask>("waitForManualJudgment")
    }

    // Apply
    appendTerraformStage("terraformApply", stage, builder)
  }

  private fun appendTerraformStage(
    innerJobName: String,
    stage: StageExecution,
    builder: TaskNode.Builder
  ) {
    val jobSuffix = innerJobName.capitalize()

    builder.withTask<RunInnerJobTask>("runInnerJob$jobSuffix")
    builder.withTask<MonitorInnerJobTask>("monitorJob$jobSuffix")
    builder.withTask<PromoteInnerOutputsTask>("promoteInnerOutputs$jobSuffix")
    builder.withTask<WaitOnInnerJobCompletionTask>("waitOnInnerJobCompletion$jobSuffix")

    getInnerJobContext(stage, innerJobName)?.let { innerJobContext ->
      if (innerJobContext.containsKey("expectedArtifacts")) {
        builder.withTask<BindInnerProducedArtifactsTask>("bindInnerProducedArtifacts$jobSuffix")
      }

      if (innerJobContext
          .getOrDefault("consumeArtifactSource", "")
          .toString()
          .equals("artifact", ignoreCase = true)
      ) {
        builder.withTask<ConsumeInnerArtifactTask>("consumeInnerArtifact$jobSuffix")
      }
    }
  }

  private fun prepareInnerJobForRestart(stage: StageExecution, innerJobName: String) {
    val clearedContext =
      getInnerJobContext(stage, innerJobName)?.filterKeys {
        !INNER_KEYS_TO_CLEAR_ON_RESTART.contains(it)
      }

    clearedContext?.let { stage.context[innerJobName] = it }
  }

  override fun prepareStageForRestart(stage: StageExecution) {
    super.prepareStageForRestart(stage)

    stage.context["currentInnerJob"] = "terraformPlan"
    listOf("terraformPlan", "terraformApply").forEach { prepareInnerJobForRestart(stage, it) }
  }
}
