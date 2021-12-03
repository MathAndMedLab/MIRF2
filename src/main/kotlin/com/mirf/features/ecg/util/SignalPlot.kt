package com.mirf.features.ecg.util

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Stroke
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException

class SignalPlot {
    var font: Font
    var chart: JFreeChart? = null
    var colors: ArrayList<Color>
    var strokes: ArrayList<Stroke>
    var dataset: XYSeriesCollection?

    init {
        font = JFreeChart.DEFAULT_TITLE_FONT
        colors = ArrayList()
        strokes = ArrayList()
        dataset = XYSeriesCollection()
    }


    fun plot(x: DoubleArray, y: DoubleArray, lineType: String, color: Color, lineWidth: Float) {
        val series = XYSeries("")
        for (i in x.indices)
            series.add(x[i], y[i])
        dataset!!.addSeries(series)
        colors.add(color)
        strokes.add(this.getStroke(lineType, lineWidth))

        var chart: JFreeChart? = null
        if (dataset != null && dataset!!.seriesCount > 0)
            chart =
                ChartFactory.createXYLineChart(null, null, null, dataset, PlotOrientation.VERTICAL, false, false, false)
        else
            println("Plot is empty")

        val plot = chart!!.xyPlot
        for (i in colors.indices) {
            plot.renderer.setSeriesPaint(i, colors[i])
            plot.renderer.setSeriesStroke(i, strokes[i])
        }
        (plot.domainAxis as NumberAxis).autoRangeIncludesZero = false
        (plot.rangeAxis as NumberAxis).autoRangeIncludesZero = false

        plot.backgroundPaint = Color.WHITE
        plot.outlinePaint = null
        chart.removeLegend()
        this.chart = chart
    }


    fun displayAxis(xAxis: Boolean, yAxis: Boolean) {
        (chart!!.xyPlot.domainAxis as NumberAxis).isVisible = xAxis
        (chart!!.xyPlot.rangeAxis as NumberAxis).isVisible = yAxis
    }

    fun setIntervalX(l: Double, u: Double) {
        chart!!.xyPlot.domainAxis.setRange(l, u)
    }

    fun setIntervalY(l: Double, u: Double) {
        chart!!.xyPlot.rangeAxis.setRange(l, u)
    }

    fun setLabelX(label: String?) {
        chart!!.xyPlot.domainAxis.label = label
    }

    fun setLabelY(label: String?) {
        chart!!.xyPlot.rangeAxis.label = label
    }


    fun displayGrid(xAxis: Boolean, yAxis: Boolean) {
        if (xAxis) {
            chart!!.xyPlot.isDomainGridlinesVisible = true
            chart!!.xyPlot.isDomainMinorGridlinesVisible = true
            chart!!.xyPlot.domainGridlinePaint = Color.GRAY
        } else {
            chart!!.xyPlot.isDomainGridlinesVisible = false
            chart!!.xyPlot.isDomainMinorGridlinesVisible = false
        }

        if (yAxis) {
            chart!!.xyPlot.isRangeGridlinesVisible = true
            chart!!.xyPlot.isRangeMinorGridlinesVisible = true
            chart!!.xyPlot.rangeGridlinePaint = Color.GRAY
        } else {
            chart!!.xyPlot.isRangeGridlinesVisible = false
            chart!!.xyPlot.isRangeMinorGridlinesVisible = false
        }
    }

    fun saveAsJpeg(fileName: String?, width: Int, height: Int) {
        val file = File(fileName!!)
        try {
            ChartUtilities.saveChartAsJPEG(file, chart, width, height)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getBufferedImage(width: Int, height: Int): BufferedImage {
        return chart!!.createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB, null)
    }

    private fun getStroke(strokeType: String, lineWidth: Float): Stroke {
        //default is "-"

        val stroke: Stroke = when (strokeType) {
            "." -> {
                val dot = floatArrayOf(lineWidth)
                BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, dot, 0.0f)
            }
            ":" -> {
                val dash = floatArrayOf(5.0f)
                BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f)
            }
            else -> BasicStroke(lineWidth)
        }

        return stroke
    }
}