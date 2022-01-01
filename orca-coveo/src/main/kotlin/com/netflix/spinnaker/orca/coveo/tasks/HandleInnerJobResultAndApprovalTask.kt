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

import com.netflix.spinnaker.orca.api.pipeline.OverridableTimeoutRetryableTask
import com.netflix.spinnaker.orca.api.pipeline.TaskResult
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.coveo.tasks.job.InnerJobAware
import com.netflix.spinnaker.orca.coveo.tasks.job.TerraformStageInnerJob
import com.netflix.spinnaker.orca.echo.util.ManualJudgmentAuthorization
import com.netflix.spinnaker.orca.ext.mapTo
import java.util.concurrent.TimeUnit
import org.springframework.stereotype.Component

enum class TerraformPlanApprovalState {
  UNAPPROVED,
  APPROVED,
  APPROVAL_DENIED,
  DENIED
}

@Component
class HandleInnerJobResultAndApprovalTask(
  private val manualJudgmentAuthorization: ManualJudgmentAuthorization
) : InnerJobAware, OverridableTimeoutRetryableTask {
  override fun execute(stage: StageExecution): TaskResult {
    // If we reach that point it's finished
    if (getCurrentInnerJobKey(stage) == TerraformStageInnerJob.APPLY.key) {
      return TaskResult.builder(ExecutionStatus.SUCCEEDED).build()
    }

    return when (getApprovalState(stage)) {
      TerraformPlanApprovalState.UNAPPROVED -> TaskResult.builder(ExecutionStatus.RUNNING).build()
      TerraformPlanApprovalState.APPROVED -> handleApprovedPlan(stage)
      TerraformPlanApprovalState.APPROVAL_DENIED -> handleDeniedApproval()
      TerraformPlanApprovalState.DENIED -> TaskResult.builder(ExecutionStatus.TERMINAL).build()
    }
  }

  private fun getApprovalState(stage: StageExecution): TerraformPlanApprovalState {
    val approvalContext = stage.mapTo<ManualApprovalContext>()
    if (approvalContext.isAutoApproved) {
      return TerraformPlanApprovalState.APPROVED
    }

    return when (approvalContext.approvalStatus) {
      ManualApprovalStatus.APPROVED -> checkApprovalAuthorization(stage, approvalContext)
      ManualApprovalStatus.DENIED -> TerraformPlanApprovalState.DENIED
      else -> TerraformPlanApprovalState.UNAPPROVED
    }
  }

  private fun checkApprovalAuthorization(
    stage: StageExecution,
    approvalContext: ManualApprovalContext
  ): TerraformPlanApprovalState {
    stage.lastModified?.user?.let { currentUser ->
      val isAuthorized =
        manualJudgmentAuthorization.isAuthorized(approvalContext.requiredJudgmentRoles, currentUser)
      if (isAuthorized) {
        return TerraformPlanApprovalState.APPROVED
      }
    }
    return TerraformPlanApprovalState.APPROVAL_DENIED
  }

  private fun handleApprovedPlan(stage: StageExecution): TaskResult {
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

    val updatedInnerJobContext =
      getUpdatedContextWithInnerJob(
        stage,
        mapOf("savedJobDetailsBeforeClear" to savedJobDetailsBeforeClear)
      )

    // ExecutionStatus.REDIRECT will restart the loop which will apply the Plan
    return TaskResult.builder(ExecutionStatus.REDIRECT)
      .context(
        mapOf("currentInnerJob" to TerraformStageInnerJob.APPLY.key) + updatedInnerJobContext
      )
      .build()
  }

  private fun handleDeniedApproval(): TaskResult {
    return TaskResult.builder(ExecutionStatus.RUNNING)
      .context(mapOf("judgmentStatus" to ""))
      .build()
  }

  override fun getBackoffPeriod(): Long = TimeUnit.SECONDS.toMillis(15)
  override fun getTimeout(): Long = TimeUnit.DAYS.toMillis(3)
}
