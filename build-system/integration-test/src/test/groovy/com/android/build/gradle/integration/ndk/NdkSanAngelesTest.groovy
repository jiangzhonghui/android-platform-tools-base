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

package com.android.build.gradle.integration.ndk

import com.android.SdkConstants
import com.android.build.FilterData
import com.android.build.OutputFile
import com.android.build.gradle.integration.common.category.DeviceTests
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.truth.TruthHelper
import com.android.build.gradle.integration.common.utils.ModelHelper
import com.android.builder.model.AndroidArtifact
import com.android.builder.model.AndroidArtifactOutput
import com.android.builder.model.AndroidProject
import com.android.builder.model.NativeLibrary
import com.android.builder.model.Variant
import com.google.common.collect.Maps
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat
import static com.android.builder.core.BuilderConstants.DEBUG
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Assemble tests for ndkSanAngeles.
 */
@CompileStatic
class NdkSanAngelesTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("ndkSanAngeles")
            .create()

    static AndroidProject model

    @BeforeClass
    static void setUp() {
        model = project.executeAndReturnModel("clean", "assembleDebug")
    }

    @AfterClass
    static void cleanUp() {
        project = null
        model = null
    }

    @Test
    void lint() {
        project.execute("lint")
    }

    @Test
    void "check version code in model"() {
        Collection<Variant> variants = model.getVariants()
        assertEquals("Variant Count", 2, variants.size())

        // get the main artifact of the debug artifact
        Variant debugVariant = ModelHelper.getVariant(variants, DEBUG)
        assertNotNull("debug Variant null-check", debugVariant)
        AndroidArtifact debugMainArtifact = debugVariant.getMainArtifact()
        assertNotNull("Debug main info null-check", debugMainArtifact)

        // get the outputs.
        Collection<AndroidArtifactOutput> debugOutputs = debugMainArtifact.getOutputs()
        assertNotNull(debugOutputs)
        assertEquals(3, debugOutputs.size())

        // build a map of expected outputs and their versionCode
        Map<String, Integer> expected = Maps.newHashMapWithExpectedSize(5)
        expected.put("armeabi-v7a", 1000123)
        expected.put("mips", 2000123)
        expected.put("x86", 3000123)

        assertEquals(3, debugOutputs.size())
        for (AndroidArtifactOutput output : debugOutputs) {
            Collection<? extends OutputFile> outputFiles = output.getOutputs()
            assertEquals(1, outputFiles.size())
            for (FilterData filterData : outputFiles.iterator().next().getFilters()) {
                if (filterData.getFilterType().equals(OutputFile.ABI)) {
                    String abiFilter = filterData.getIdentifier()
                    Integer value = expected.get(abiFilter)
                    // this checks we're not getting an unexpected output.
                    assertNotNull("Check Valid output: " + abiFilter, value)

                    assertEquals(value.intValue(), output.getVersionCode())
                    expected.remove(abiFilter)
                }
            }
        }

        // this checks we didn't miss any expected output.
        assertTrue(expected.isEmpty())
    }

    @Test
    void "check native libraries in model"() {
        Collection<Variant> variants = model.getVariants()
        Variant debugVariant = ModelHelper.getVariant(variants, DEBUG)
        AndroidArtifact debugMainArtifact = debugVariant.getMainArtifact()

        assertThat(debugMainArtifact.getNativeLibraries()).hasSize(3)
        for (NativeLibrary nativeLibrary : debugMainArtifact.getNativeLibraries()) {
            assertThat(nativeLibrary.getName()).isEqualTo("sanangeles")
            assertThat(nativeLibrary.getCCompilerFlags()).contains("-DANDROID_NDK -DDISABLE_IMPORTGL");
            assertThat(nativeLibrary.getCppCompilerFlags()).contains("-DANDROID_NDK -DDISABLE_IMPORTGL");
            assertThat(nativeLibrary.getCSystemIncludeDirs()).isEmpty();
            assertThat(nativeLibrary.getCppSystemIncludeDirs()).isNotEmpty();
        }

        Collection<String> expectedToolchains = [
                SdkConstants.ABI_INTEL_ATOM,
                SdkConstants.ABI_ARMEABI_V7A,
                SdkConstants.ABI_MIPS].collect { "gcc-" + it }
        Collection<String> toolchainNames = model.getNativeToolchains().collect { it.getName() }
        assertThat(toolchainNames).containsAllIn(expectedToolchains)
        Collection<String> nativeLibToolchains = debugMainArtifact.getNativeLibraries().collect { it.getToolchainName() }
        assertThat(nativeLibToolchains).containsAllIn(expectedToolchains)
    }

    @Test
    @Category(DeviceTests.class)
    void connectedCheck() {
        project.executeConnectedCheck()
    }
}
