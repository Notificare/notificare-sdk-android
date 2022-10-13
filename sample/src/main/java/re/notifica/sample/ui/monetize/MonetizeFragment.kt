package re.notifica.sample.ui.monetize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout
import re.notifica.monetize.NotificareMonetize
import re.notifica.sample.databinding.FragmentMonetizeBinding

class MonetizeFragment : Fragment(), NotificareMonetize.Listener {
    private lateinit var binding: FragmentMonetizeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMonetizeBinding.inflate(inflater, container, false)

        val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.simpleFrameLayout.id, MonetizeProductsFragment())
        fragmentTransaction.commit()

        setupListeners()

        return binding.root
    }

    private fun setupListeners() {
        val tabLayout = binding.monetizeTabLayout

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                var fragment: Fragment? = null
                if (tab != null) {
                    when (tab.position) {
                        0 -> fragment = MonetizeProductsFragment()
                        1 -> fragment = MonetizePurchasesFragment()
                    }
                }

                val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                if (fragment != null) {
                    fragmentTransaction.replace(binding.simpleFrameLayout.id, fragment)
                    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    fragmentTransaction.commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }
}
