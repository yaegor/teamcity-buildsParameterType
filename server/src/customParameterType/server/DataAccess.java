/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package customParameterType.server;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.ItemProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yegor.Yarko
 *         Date: 13.05.2014
 */
public class DataAccess {
  private final BuildHistory myBuildHistory;
  private final ProjectManager myProjectManager;

  public DataAccess(BuildHistory buildHistory, ProjectManager projectManager) {
    myBuildHistory = buildHistory;
    myProjectManager = projectManager;
  }

  List<SBuild> getBuilds(@NotNull SBuildType buildType){
    final ArrayList<SBuild> result = new ArrayList<SBuild>();
    myBuildHistory.processEntries(buildType.getInternalId(), null, false, false, true, new ItemProcessor<SFinishedBuild>() {
      public boolean processItem(SFinishedBuild item) {
        result.add(item);
        return result.size() <= 100;
      }
    });
    return result;
  }

  public SBuildType getBuildTypebyExternalId(String buildTypeExternalId) {
    return myProjectManager.findBuildTypeByExternalId(buildTypeExternalId);
  }

  public boolean isMatchingBuildId(SBuildType buildType, Long buildId){
    final SFinishedBuild build = myBuildHistory.findEntry(buildId);
    return build != null && build.getBuildTypeId().equals(buildType.getInternalId());
  }
}
