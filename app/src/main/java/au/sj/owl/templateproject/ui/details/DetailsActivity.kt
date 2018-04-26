package au.sj.owl.templateproject.ui.details

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.text.format.DateUtils
import android.util.TimingLogger
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import au.sj.owl.templateproject.App
import au.sj.owl.templateproject.R
import au.sj.owl.templateproject.di.detailed.DetailedRssModule
import au.sj.owl.templateproject.di.rssall.RssAllModule
import au.sj.owl.templateproject.ui.home.dataholder.DataHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_details.progressBar
import kotlinx.android.synthetic.main.activity_details.rssdetFab
import kotlinx.android.synthetic.main.activity_details.rssdetIcon
import kotlinx.android.synthetic.main.activity_details.rssdetPubDate
import kotlinx.android.synthetic.main.activity_details.rssdetTitle
import kotlinx.android.synthetic.main.activity_details.rssdetWebView
import kotlinx.android.synthetic.main.activity_details.rssdetsCsCont
import kotlinx.android.synthetic.main.activity_details.toolbar
import timber.log.Timber
import javax.inject.Inject


class DetailsActivity : AppCompatActivity() {

    @Inject
    lateinit var iDetailedRssPresenter: IDetailedRssPresenter

    /**
     * ==========================        user actions with this activity        ==========================
     */
    fun openInBrowser() {
        Timber.e("jsp open in browser!")
        val url = rssItem.link
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        startActivity(i)
    }

    /**
     * ====================================================================================================
     */

    private lateinit var rssItem: DataHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        val timings = TimingLogger("sj", "detailedActivity")

        super.onCreate(savedInstanceState)
        timings.addSplit("super.onCreate(savedInstanceState)")
        setContentView(R.layout.activity_details)
        timings.addSplit("setContentView(R.layout.activity_details)")
        setSupportActionBar(toolbar)
        timings.addSplit("setSupportActionBar(toolbar)")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        timings.addSplit("supportActionBar?.setDisplayHomeAsUpEnabled(true)")
        App.get(applicationContext).applicationComponent()
                .plus(RssAllModule())
                .plus(DetailedRssModule())
                .inject(this)
        timings.addSplit("injection")

        setUpContent()
        timings.addSplit("setUpContent()")

        timings.dumpToLog()
    }

    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
    }
    //
    //    override fun onPause() {
    //        (rssdetWebView.parent as ViewGroup).removeView(rssdetWebView)
    //        rssdetWebView.destroy()
    //
    //        super.onPause()
    //    }

    /**
     * ==========================        init views        ==========================
     */
    private fun setFab() {
        setFabIcon(rssItem.bookmarked)
        rssdetFab.setOnClickListener { iDetailedRssPresenter.bookmark() }
        iDetailedRssPresenter.onBookmarked()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { bookmarked -> setFabIcon(bookmarked) }
    }

    private fun setFabIcon(enabled: Boolean) {
        var d = if (enabled) R.drawable.ic_bookmarkyes else R.drawable.ic_bookmarkno
        rssdetFab.setImageDrawable(resources.getDrawable(d))
    }

    private fun setTitle() {
        title = ""
        rssdetTitle.text = rssItem.title
    }

    private fun setDate() {
        rssdetPubDate.text = DateUtils.formatDateTime(this,
                                                      rssItem.date,
                                                      DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE)

    }

    private fun setContent() {

        rssdetWebView.webViewClient = WebViewClient()
        rssdetWebView.loadUrl(rssItem.link)
        progressBar.visibility = View.VISIBLE
        rssdetWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?,
                                           newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE

                    val cs = ConstraintSet()
                    cs.clone(rssdetsCsCont)
                    cs.constrainHeight(rssdetWebView.id, ConstraintSet.WRAP_CONTENT)
                    cs.applyTo(rssdetsCsCont)
                }
            }
        }
    }

    private fun setIcon() {
        if (rssItem.imgUrl != "") {
            rssdetIcon.transitionName = rssItem.imgUrl
            rssdetIcon.setOnClickListener { finishAfterTransition() }
            postponeEnterTransition()
            Glide.with(this)
                    .load(rssItem.imgUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?,
                                                  model: Any?,
                                                  target: Target<Drawable>?,
                                                  isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?,
                                                     model: Any?,
                                                     target: Target<Drawable>?,
                                                     dataSource: DataSource?,
                                                     isFirstResource: Boolean): Boolean {
                            startPostponedEnterTransition()
                            return false
                        }

                    })
                    .into(rssdetIcon)
        } else {
            rssdetIcon.minimumHeight = 0
        }

    }

    private fun setUpContent() {
        rssItem = intent.getParcelableExtra("dataHolder")
        iDetailedRssPresenter.bindLink(rssItem.link)
        setFab()
        setTitle()
        setDate()
        setContent()
        setIcon()
    }


    /**
     * ==========================        menu        ==========================
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.menu_browser -> openInBrowser()
            else              -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
