package never.ending.splendor.app.ui

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import never.ending.splendor.R

class ShowPagerAdapter(
    private val mRootView: View
) : PagerAdapter() {

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        var view: View? = null
        if (position == 0) view = mRootView.findViewById(R.id.tracks)
        if (position == 1) view = mRootView.findViewById(R.id.setlist)
        if (position == 2) view = mRootView.findViewById(R.id.reviews)
        if (position == 3) view = mRootView.findViewById(R.id.tapernotes)
        collection.addView(view)
        return view!!
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return 4
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> return "Tracks"
            1 -> return "Setlist"
            2 -> return "Reviews"
            3 -> return "Taper Notes"
        }
        return null
    }
}
