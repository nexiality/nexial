package org.nexial.core.plugins.image

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.ImageComparisonUtil
import com.github.romankh3.image.comparison.model.ImageComparisonState.MATCH
import org.apache.commons.lang3.SystemUtils.JAVA_IO_TMPDIR
import org.junit.Test
import org.nexial.commons.utils.FileUtil
import org.nexial.commons.utils.ResourceUtils
import java.io.File
import java.io.File.separator
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class ImageComparisonTest {

    @Test
    fun testImageComparison() {
        val resultBase = "$JAVA_IO_TMPDIR${separator}nexial${separator}img${separator}"
        File(resultBase).mkdirs()
        FileUtil.deleteFiles(resultBase, ".+\\.png", false, false)

        val imageBase = ResourceUtils.getResourceFilePath("/unittesting/artifact/data/image") + "/unitTest_ImageCompare"

        // load the images to be compared
        compare("${imageBase}1.actual.png", "${imageBase}1.expected.png", "${resultBase}result1.png", true)
        compare("${imageBase}2.actual.png", "${imageBase}2.expected.png", "${resultBase}result2.png", true)
        compare("${imageBase}3.actual.png", "${imageBase}3.expected.png", "${resultBase}result3.png", false)
        compare("${imageBase}3a.actual.png", "${imageBase}3.expected.png", "${resultBase}result3a.png", true)
        compare("${imageBase}3b.actual.png", "${imageBase}3.expected.png", "${resultBase}result3b.png", true)
        compare("${imageBase}4.actual.png", "${imageBase}4.expected.png", "${resultBase}result4.png", true)
        compare("${imageBase}4a.actual.png", "${imageBase}4.expected.png", "${resultBase}result4a.png", true)
        compare("${imageBase}4b.actual.png", "${imageBase}4.expected.png", "${resultBase}result4b.png", false)
    }

    private fun compare(actual: String, expected: String, result: String, expectMatch: Boolean) {
        val expectedImage = ImageComparisonUtil.readImageFromResources(actual)
        val actualImage = ImageComparisonUtil.readImageFromResources(expected)

        // where to save the result (leave null if you want to see the result in the UI)
        val resultDestination = File(result)

        //Create ImageComparison object for it.
        val imageComparison = ImageComparison(expectedImage, actualImage, resultDestination)

        imageComparison.rectangleLineWidth = 3
        imageComparison.setDifferenceRectangleFilling(true, 5.0)
        imageComparison.setExcludedRectangleFilling(true, 5.0)

        //Also can be configured BEFORE comparing next properties:
        //Threshold - it's the max distance between non-equal pixels. By default it's 5.
        imageComparison.threshold = 6

        //Change the level of the pixel tolerance:
        imageComparison.pixelToleranceLevel = 0.12
        // imageComparison.allowingPercentOfDifferentPixels = 0.1
        imageComparison.minimalRectangleSize = 144

        //After configuring the ImageComparison object, can be executed compare() method:
        val imageComparisonResult = imageComparison.compareImages()

        //Can be found ComparisonState.
        val imageComparisonState = imageComparisonResult.imageComparisonState
        println("imageComparisonState = $imageComparisonState")

        //Image can be saved after comparison, using ImageComparisonUtil.
        if (imageComparisonState != MATCH) {
            println("expected image = ${imageComparison.expected.width},${imageComparison.expected.height}")
            println("  actual image = ${imageComparison.actual.width},${imageComparison.actual.height}")
            println("diff percentage = ${imageComparisonResult.differencePercent}")
            if (imageComparisonResult.rectangles != null) {
                println(imageComparisonResult.rectangles
                                .filterNotNull()
                                .map { "diffs = (x,y,w,h): (${it.minPoint.x},${it.minPoint.y}),(${it.maxPoint.x},${it.maxPoint.y}),${it.width},${it.height},[${it.size()}]\n" })
            }

            //And Result Image
            val resultImage = imageComparisonResult.result

            ImageComparisonUtil.saveImage(resultDestination, resultImage)
            println("comparison result = $resultDestination")
            println()
            assertFalse("expects $actual to be difference than $expected") { expectMatch }
        } else {
            assertTrue("expects $actual to be the same as $expected") { expectMatch }
        }
    }
}