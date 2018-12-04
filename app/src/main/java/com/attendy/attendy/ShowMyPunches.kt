package com.attendy.attendy

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.sun.xml.internal.fastinfoset.alphabet.BuiltInRestrictedAlphabets.table
import android.widget.TableLayout
import android.widget.TableRow
import com.attendy.attendy.R.id.text
import kotlinx.android.synthetic.main.activity_show_my_punches.*


class ShowMyPunches : AppCompatActivity() {
//https://www.androidly.net/136/android-tablelayout-using-kotlin
    val rows = 7
    val columns = 4
    //lazy initialization’ was designed to prevent unnecessary initialization of objects
    val tableLayout by lazy { TableLayout(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_my_punches)

        RandomtextView.text = "ROWS : $rows COLUMNS: $columns"

        val lp = TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        tableLayout.apply {
            layoutParams = lp
            isShrinkAllColumns = true
        }

        createTable(rows, columns)


    }

    fun createTable(rows: Int, cols: Int) {

        for (i in 0 until rows) {

            val row = TableRow(this)
            row.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)

            for (j in 0 until cols) {

                val button = Button(this)
                button.apply {
                    layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                            TableRow.LayoutParams.WRAP_CONTENT)
                    text = "R $i C $j"
                }
                row.addView(button)
            }
            tableLayout.addView(row)
        }
        LinearLayout.addView(tableLayout)
    }


}
