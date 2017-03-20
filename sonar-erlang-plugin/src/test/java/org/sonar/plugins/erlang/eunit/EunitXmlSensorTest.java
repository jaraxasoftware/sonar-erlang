/*
 * SonarQube Erlang Plugin
 * Copyright (C) 2012-2017 Tamas Kende
 * kende.tamas@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.erlang.eunit;

import com.google.common.base.Charsets;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.erlang.ErlangPlugin;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EunitXmlSensorTest {

  private File testModuleBasedir = new File("src/test/resources/org/sonar/plugins/erlang/erlcount/");
  private Settings settings;
  private SensorContextTester context;

  @Before
  public void setup() throws URISyntaxException {
    settings = new Settings(new PropertyDefinitions(ErlangPlugin.class));
    context = SensorContextTester.create(testModuleBasedir);
    settings.setProperty(ErlangPlugin.EUNIT_FOLDER_KEY, ErlangPlugin.EUNIT_DEFAULT_FOLDER);
    settings.setProperty(ErlangPlugin.DIALYZER_FILENAME_KEY, ErlangPlugin.DIALYZER_DEFAULT_FILENAME);
    context.setSettings(settings);

    addFile(context, "test/erlcount_tests.erl");
    addFile(context, ".eunit/TEST-erlcount_tests.xml");
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.<Integer>findByKey(CoreMetrics.TESTS_KEY)).thenReturn(CoreMetrics.TESTS);
    when(metricFinder.<Integer>findByKey(CoreMetrics.SKIPPED_TESTS_KEY)).thenReturn(CoreMetrics.SKIPPED_TESTS);
    when(metricFinder.<Integer>findByKey(CoreMetrics.TEST_ERRORS_KEY)).thenReturn(CoreMetrics.TEST_ERRORS);
    when(metricFinder.<Integer>findByKey(CoreMetrics.TEST_FAILURES_KEY)).thenReturn(CoreMetrics.TEST_FAILURES);
    when(metricFinder.<Long>findByKey(CoreMetrics.TEST_EXECUTION_TIME_KEY)).thenReturn(CoreMetrics.TEST_EXECUTION_TIME);
    when(metricFinder.<Double>findByKey(CoreMetrics.TEST_SUCCESS_DENSITY_KEY)).thenReturn(CoreMetrics.TEST_SUCCESS_DENSITY);
    new EunitXmlSensor(metricFinder).execute(context);
  }

  private void addFile(SensorContextTester context, String path) {
    DefaultInputFile file = new DefaultInputFile("test", path)
            .setLanguage("erlang")
            .setType(InputFile.Type.TEST)
            .setModuleBaseDir(testModuleBasedir.toPath());
    file.initMetadata(new FileMetadata().readMetadata(file.file(), Charsets.UTF_8));
    context.fileSystem().add(file);
  }

  @Test
  public void shouldSaveErrorsAndFailuresInXML() throws URISyntaxException {
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.TESTS_KEY).value()).isEqualTo(7);
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.SKIPPED_TESTS_KEY).value()).isEqualTo(0);
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.TEST_ERRORS_KEY).value()).isEqualTo(0);
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.TEST_FAILURES_KEY).value()).isEqualTo(1);
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.TEST_EXECUTION_TIME_KEY).value()).isEqualTo(133L);
    assertThat(context.measure("test:test/erlcount_tests.erl", CoreMetrics.TEST_SUCCESS_DENSITY_KEY).value()).isEqualTo(1/7.0 );
  }
}
