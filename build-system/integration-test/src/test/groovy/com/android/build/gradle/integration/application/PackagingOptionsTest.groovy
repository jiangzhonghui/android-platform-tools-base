/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.integration.application
import com.android.build.gradle.integration.common.category.DeviceTests
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatZip
/**
 * Assemble tests for packagingOptions.
 */
@CompileStatic
class PackagingOptionsTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("packagingOptions")
            .create()

    @BeforeClass
    static void setUp() {
        project.execute("clean", "assembleDebug")
    }

    @AfterClass
    static void cleanUp() {
        project = null
    }

    @Test
    void lint() {
        project.execute("lint")
    }

    @Test
    void "check packinging"() {
        assertThatZip(project.getApk("debug")).contains("first_pick.txt")
        assertThatZip(project.getApk("debug")).doesNotContain("excluded.txt")

        assertThatZip(project.getApk("debug")).contains("lib/x86/libdummy.so")
    }

    @Test
    @Category(DeviceTests.class)
    void connectedCheck() {
        project.executeConnectedCheck()
    }
}
