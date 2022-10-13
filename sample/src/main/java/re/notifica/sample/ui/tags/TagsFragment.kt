package re.notifica.sample.ui.tags

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import re.notifica.sample.R
import re.notifica.sample.databinding.FragmentTagsBinding
import re.notifica.sample.models.BaseFragment

class TagsFragment : BaseFragment() {
    private lateinit var binding: FragmentTagsBinding
    private val viewModel: TagsViewModel by viewModels()

    override val baseViewModel: TagsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tags_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> onRefreshClicked()
            R.id.remove_all -> onRemoveAllClicked()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTagsBinding.inflate(inflater, container, false)

        setupListeners()
        setupObservers()
        onRefreshClicked()

        return binding.root
    }

    private fun setupListeners() {
        binding.addTagsButton.setOnClickListener {
            onAddButtonClicked()
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

    private fun onAddButtonClicked() {
        binding.tagsChipGroup.checkedChipIds.forEach { id ->
            val chip = binding.tagsChipGroup.findViewById<Chip>(id)
            viewModel.selectedTags.add(chip.text.toString())
            chip.isChecked = false
        }

        val inputTag = binding.manualTagInput.editText?.text
        if (!inputTag.isNullOrEmpty()) {
            viewModel.selectedTags.add(inputTag.toString())
            inputTag.clear()
        }

        if (viewModel.selectedTags.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage("Select tags or write your own.")
                .setCancelable(false)
                .setNegativeButton(R.string.dialog_ok_button) { _, _ ->
                }
                .show()

            return
        }

        viewModel.addTags()
    }

    private fun onRefreshClicked() {
        viewModel.fetchTags()
    }

    private fun onRemoveAllClicked() {
        viewModel.clearTags()
    }
}
