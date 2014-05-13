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

import jetbrains.buildServer.controllers.parameters.InvalidParametersException;
import jetbrains.buildServer.controllers.parameters.ParameterContext;
import jetbrains.buildServer.controllers.parameters.ParameterEditContext;
import jetbrains.buildServer.controllers.parameters.ParameterRenderContext;
import jetbrains.buildServer.controllers.parameters.api.ParameterControlProviderAdapter;
import jetbrains.buildServer.serverSide.ControlDescription;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author Yegor.Yarko
 *         Date: 13.05.2014
 */
public class CustomParameterSelect extends ParameterControlProviderAdapter {

  protected static final String BUILD_TYPE_EXTERNAL_ID = "buildTypeId";
  protected static final String SEPARATOR = ":";
  protected static final String EXAMPLE = "\"" + BUILD_TYPE_EXTERNAL_ID + SEPARATOR + "<ID>\"";
  protected static final String EMPTY = "";
  private final DataAccess myDataAccess;
  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  private static final String DETAILS = "details";

  public CustomParameterSelect(DataAccess dataAccess, @NotNull PluginDescriptor pluginDescriptor) {
    myDataAccess = dataAccess;
    myPluginDescriptor = pluginDescriptor;
  }

  @Override
  @NotNull
  public String getParameterType() {
    return "build";
  }

  @NotNull
  @Override
  public String getParameterDescription() {
    return "Build";
  }

  @Override
  public void validateParameterDescription(@NotNull final ParameterContext context) throws InvalidParametersException {
    super.validateParameterDescription(context);
    parseItems(context.getDescription());
  }

  @Override
  @NotNull
  public ModelAndView renderControl(@NotNull final HttpServletRequest request,
                                    @NotNull final ParameterRenderContext context) throws InvalidParametersException {
    final ControlDescription descr = context.getDescription();
    ModelAndView mv = new ModelAndView(myPluginDescriptor.getPluginResourcesPath("/optionsField.jsp"));
    mv.getModel().put("options", parseItems(descr));
    return mv;
  }

  @NotNull
  @Override
  public ModelAndView renderSpecEditor(@NotNull final HttpServletRequest request, @NotNull final ParameterEditContext context)
          throws InvalidParametersException {
    final ModelAndView mw = new ModelAndView(myPluginDescriptor.getPluginResourcesPath("/editBuildTypeChooser.jsp"));
    mw.getModel().put(DETAILS, serializeItems(context.getDescription()));
    return mw;
  }

  @NotNull
  @Override
  public Map<String, String> convertSpecEditorParameters(@NotNull final Map<String, String> parameters) throws InvalidParametersException {
    final Map<String, String> result = new HashMap<String, String>(parameters);

    result.remove(DETAILS);
    final String details = parameters.get(DETAILS);
    if (details == null) {
      throw new InvalidParametersException("Empty \"" + DETAILS + "\" field. Should contain " + EXAMPLE + " line");
    }

    final Map<String, String> parsed = parse(details);
    if (parsed.isEmpty()) {
      throw new InvalidParametersException("There should be at least one line with the syntax " + EXAMPLE);
    }

    result.putAll(parsed);
    return result;
  }

  @Override
  public final void validateDefaultParameterValue(@NotNull final ParameterContext context, @Nullable final String value)
          throws InvalidParametersException {
    validate(context.getDescription(), value);
  }

  @NotNull
  @Override
  public final Collection<InvalidProperty> validateParameterValue(@NotNull final HttpServletRequest request,
                                                                  @NotNull final ParameterRenderContext context,
                                                                  @Nullable final String value) throws InvalidParametersException {
    validate(context.getDescription(), value);
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<InvalidProperty> validateSpecEditorParameters(@NotNull final Map<String, String> properties) {
    return super.validateSpecEditorParameters(properties);
  }




  @NotNull
  protected List<KeyValue> parseItems(@NotNull ControlDescription controlDescription) throws InvalidParametersException {
    final List<KeyValue> result = new ArrayList<KeyValue>();
    final SBuildType buildType = getBuildType(controlDescription);
    for (SBuild build : myDataAccess.getBuilds(buildType)) {
      result.add(new KeyValue(String.valueOf(build.getBuildId()), getBuildDescription(build)));
    }
    return result;
  }

  protected String serializeItems(@NotNull ControlDescription context) {
    final String existingBuildTypeId = context.getParameterTypeArguments().get(BUILD_TYPE_EXTERNAL_ID);
    return BUILD_TYPE_EXTERNAL_ID + SEPARATOR + (existingBuildTypeId != null ? existingBuildTypeId : "<ID>");
  }

  protected void validate(@NotNull final ControlDescription controlDescription, @Nullable String value) throws InvalidParametersException {
    Long buildId;
    try {
      buildId = Long.valueOf(value);
    } catch (NumberFormatException e) {
      throw new InvalidParametersException("Value \"" + value + "\" is not a number");
    }

    final SBuildType buildType = getBuildType(controlDescription);
    if (!myDataAccess.isMatchingBuildId(buildType, buildId)){
      throw new InvalidParametersException("Build id \"" + buildId + "\" does not correspond to any build");
    }
  }

  @NotNull
  private Map<String, String> parse(@NotNull String spec) {
    final HashMap<String, String> result = new HashMap<String, String>();
    if (StringUtil.isEmptyOrSpaces(spec)) return result;

    for (String line : spec.split("[\r\n]+")) {
      if (StringUtil.isEmptyOrSpaces(line) || line.startsWith("#")) continue;

      final int ix = line.indexOf(SEPARATOR);
      if (ix >= 0) {
        result.put(line.substring(0, ix).trim(), line.substring(ix + SEPARATOR.length()).trim());
      } else {
        result.put(line.trim(), EMPTY);
      }
    }
    return result;
  }

  @NotNull
  private SBuildType getBuildType(ControlDescription context) throws InvalidParametersException {
    final Map<String, String> arguments = context.getParameterTypeArguments();
    final String buildTypeExternalId = arguments.get(BUILD_TYPE_EXTERNAL_ID);

    if (buildTypeExternalId == null || buildTypeExternalId.isEmpty()) {
      throw new InvalidParametersException("No valid build configuration id via " + EXAMPLE + " is specified");
    }

    final SBuildType result = myDataAccess.getBuildTypebyExternalId(buildTypeExternalId);
    if (result == null) {
      throw new InvalidParametersException("Build configuration not found by the specified " + BUILD_TYPE_EXTERNAL_ID + " \"" + buildTypeExternalId + "\"");
    }
    return result;
  }

  private String getBuildDescription(SBuild build) {
    final StringBuilder result = new StringBuilder();
    result.append("#").append(build.getBuildNumber());
    final List<String> tags = build.getTags();
    if (tags != null && tags.size() > 0) {
      result.append(" ").append(Arrays.toString(tags.toArray()));
    }
    if (build.getBuildStatus().isFailed()) {
      result.append(" (").append("x").append(")");
    }
    if (build.getBuildStatus().isSuccessful()) {
      result.append(" (").append("v").append(")");
    }
    result.append(" ").append(build.getStatusDescriptor().getText());

    return result.toString();
  }

  public static class KeyValue {
    private final String myKey;
    private final String myValue;

    public KeyValue(@NotNull final String key, @NotNull final String value) {
      myKey = key;
      myValue = value;
    }

    @NotNull
    public String getKey() {
      return myKey;
    }

    @NotNull
    public String getValue() {
      return myValue;
    }
  }

}
