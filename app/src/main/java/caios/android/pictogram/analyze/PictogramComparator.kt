package caios.android.pictogram.analyze

import android.content.Context
import android.util.Log
import android.util.Size
import caios.android.pictogram.R
import caios.android.pictogram.utils.LogUtils.TAG
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs
import kotlin.math.sqrt

class PictogramComparator(
    private val context: Context,
    private val event: PictogramEvent
) {

    private val pictogramData = loadPictogramData()

    private fun loadPictogramData(): PictogramData {
        val json = context.resources.openRawResource(R.raw.pictogram).bufferedReader().use { it.readText() }
        val dataList = Gson().fromJson<Collection<PictogramData>>(json, object : TypeToken<Collection<PictogramData>>() {}.type)

        // なかったら死ぬ
        return dataList.find { it.eventName.equals(pictogramEventName[event], ignoreCase = true) }!!
    }

    private fun getDistance(point1: Position, point2: Position): Float {
        val x = abs(point1.x.toFloat() - point2.x)
        val y = abs(point1.y.toFloat() - point2.y)
        return sqrt(x * x + y * y)
    }

    private fun getSlope(point1: Position, point2: Position): Float {
        val largePoint = if(point1.x > point2.x) point1 else point2
        val smallPoint = if(point1.x > point2.x) point2 else point1

        val x = largePoint.x.toFloat() - smallPoint.x
        val y = largePoint.y.toFloat() - smallPoint.y

        return y / x
    }

    private fun comparateVector(pointX1: Position, pointX2: Position, pointY1: Position, pointY2: Position): Float {
        // 距離は体格によって違う場合があるので廃止
        // val xDistance = getDistance(pointX1, pointX2)
        // val yDistance = getDistance(pointY1, pointY2)

        val xSlope = getSlope(pointX1, pointX2)
        val ySlope = getSlope(pointY1, pointY2)

        return abs(1 - (xSlope / ySlope))
    }

    fun comparate(keyPoints: List<KeyPoint>, viewSize: Size): Float {
        val scaleX = viewSize.width.toFloat() / pictogramData.imageSize.width
        val scaleY = viewSize.height.toFloat() / pictogramData.imageSize.height
        var scoreSum = 0f

        if(keyPoints.size < 10) return 10f

        for(line in bodyPartsJoint) {
            val aFirstPoint = keyPoints.find { it.bodyPart.name.equals(line.first.name, ignoreCase = true) } ?: continue
            val aSecondPoint = keyPoints.find { it.bodyPart.name.equals(line.second.name, ignoreCase = true)} ?: continue

            val cFirstPoint = pictogramData.bodyPoint.find { it.partsName.equals(bodyPartsName[line.first], ignoreCase = true) } ?: continue
            val cSecondPoint = pictogramData.bodyPoint.find { it.partsName.equals(bodyPartsName[line.second], ignoreCase = true) } ?: continue

            scoreSum += comparateVector(
                aFirstPoint.position,
                aSecondPoint.position,
                Position((cFirstPoint.x * scaleX).toInt(), (cFirstPoint.y * scaleY).toInt()),
                Position((cSecondPoint.x * scaleX).toInt(), (cSecondPoint.y * scaleY).toInt())
            )
        }

        return (scoreSum / bodyPartsJoint.size).coerceAtMost(10f)
    }

    private data class PictogramData(
        val eventName: String,
        val imageSize: ImageSize,
        val facePoint: BodyPoint,
        val bodyPoint: List<BodyPoint>
    )

    private data class BodyPoint(
        val partsName: String,
        val x: Float,
        val y: Float
    )

    private data class ImageSize(
        val width: Float,
        val height: Float
    )
}