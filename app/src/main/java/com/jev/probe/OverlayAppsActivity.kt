package com.jev.probe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jev.probe.core.Prefs
import java.text.Collator

/** Launcher-query visibility is sufficient; no QUERY_ALL_PACKAGES permission. */
class OverlayAppsActivity : AppCompatActivity() {
    private data class App(val pkg: String, val label: String)
    private lateinit var prefs: Prefs
    private val selected = mutableSetOf<String>()
    private var apps = emptyList<App>()
    private var visible = emptyList<App>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        selected.addAll(savedInstanceState?.getStringArrayList("selection") ?: prefs.overlayApps)
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * dp).toInt(), (16 * dp).toInt(), (18 * dp).toInt(), (16 * dp).toInt())
        }
        root.padForSystemBars()
        root.addView(TextView(this).apply { text = "选择启用悬浮窗的 App"; textSize = 22f })
        root.addView(TextView(this).apply {
            text = "只在勾选的 App 显示；离开后隐藏。未适配的 App 可长按悬浮球手动识别。列表为当前用户可启动的应用。"
        })
        val search = EditText(this).apply { hint = "搜索名称或包名"; isSingleLine = true }
        root.addView(search)
        val count = TextView(this)
        fun updateCount() { count.text = "已选择 ${selected.size} 个 App（未选择任何 App 时全部关闭）" }
        root.addView(count)
        val list = ListView(this).apply { choiceMode = ListView.CHOICE_MODE_MULTIPLE }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        fun render() {
            val query = search.text.toString().trim()
            visible = apps.filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
            list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice,
                visible.map { "${it.label}\n${it.pkg}" })
            visible.forEachIndexed { i, app -> list.setItemChecked(i, app.pkg in selected) }
            updateCount()
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val pkg = visible[position].pkg
            if (list.isItemChecked(position)) selected.add(pkg) else selected.remove(pkg)
            updateCount()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render() }
            override fun afterTextChanged(s: Editable?) {}
        })
        val shortcuts = LinearLayout(this)
        shortcuts.addView(Button(this).apply {
            text = "仅微信"
            setOnClickListener { selected.clear(); selected.add("com.tencent.mm"); render() }
        })
        shortcuts.addView(Button(this).apply {
            text = "全部取消"; setOnClickListener { selected.clear(); render() }
        })
        root.addView(shortcuts)
        root.addView(Button(this).apply {
            text = "保存并生效"
            setOnClickListener {
                prefs.overlayApps = selected
                Toast.makeText(this@OverlayAppsActivity, "已保存 App 使用范围", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        setContentView(root)
        updateCount()
        Thread {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = packageManager.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map { App(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
                .distinctBy { it.pkg }
            val collator = Collator.getInstance()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                // Keep saved entries visible even after uninstalling an application.
                val missing = selected.filter { pkg -> found.none { it.pkg == pkg } }
                    .map { App(it, if (it == "com.tencent.mm") "微信（未检测到）" else "未检测到的 App") }
                apps = (found + missing).sortedWith { a, b -> collator.compare(a.label, b.label) }
                render()
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("selection", ArrayList(selected))
        super.onSaveInstanceState(outState)
    }
}
