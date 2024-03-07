package re.notifica.sample.ui.tags

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.chip.Chip
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentTagsBinding
import re.notifica.sample.core.BaseFragment

class TagsFragment : BaseFragment() {
    private lateinit var binding: FragmentTagsBinding
    private val viewModel: TagsViewModel by viewModels()

    override val baseViewModel: TagsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTagsBinding.inflate(inflater, container, false)

        setupListeners()
        setupObservers()
        fetchTags()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                // Handle for example visibility of menu items
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.tags_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.refresh -> fetchTags()
                    R.id.remove_all -> clearTags()
                }

                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupListeners() {
        binding.addTagsButton.setOnClickListener {
            addTags()
        }
    }

    private fun setupObservers() {
        viewModel.defaultTags.forEach { tag ->
            val chip = this.layoutInflater.inflate(R.layout.chip_choice, binding.root, false) as Chip
            chip.text = tag
            chip.isCheckedIconVisible = true

            binding.tagsChipGroup.addView(chip)
        }

        viewModel.fetchedTag.observe(viewLifecycleOwner) { tags ->
            binding.noTagsFoundLabel.isVisible = tags.isNullOrEmpty()

            if (binding.deviceTagsChipGroup.childCount > 0) {
                binding.deviceTagsChipGroup.removeAllViews()
            }

            tags.forEach { tag ->
                val chip = this.layoutInflater.inflate(R.layout.chip_input, binding.root, false) as Chip
                chip.text = tag
                chip.tag = tag
                chip.isCloseIconVisible = true
                chip.setOnCloseIconClickListener {
                    viewModel.removeTag(tag)
                }

                binding.deviceTagsChipGroup.addView(chip)

            }
        }
    }

    private fun addTags() {
        val selectedTags = mutableListOf<String>()

        binding.tagsChipGroup.checkedChipIds.forEach { id ->
            val chip = binding.tagsChipGroup.findViewById<Chip>(id)
            selectedTags.add(chip.text.toString())
            chip.isChecked = false
        }

        val inputTag = binding.manualTagInput.editText?.text
        if (!inputTag.isNullOrEmpty()) {
            selectedTags.add(inputTag.toString())
            inputTag.clear()
        }

        if (selectedTags.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage("Select tags or write your own.")
                .setCancelable(false)
                .setNegativeButton(R.string.dialog_ok_button, null)
                .show()

            return
        }

        viewModel.addTags(selectedTags)
    }

    private fun fetchTags() {
        viewModel.fetchTags()
    }

    private fun clearTags() {
        viewModel.clearTags()
    }
}
