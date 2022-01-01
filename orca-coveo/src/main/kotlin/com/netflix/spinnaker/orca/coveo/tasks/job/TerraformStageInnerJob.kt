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

package com.netflix.spinnaker.orca.coveo.tasks.job

import com.fasterxml.jackson.annotation.JsonProperty

enum class TerraformStageInnerJob(@JsonProperty val key: String): InnerJob {
  PLAN("terraformPlan"),
  APPLY("terraformApply");

  override val innerJobKey: String = key
}
