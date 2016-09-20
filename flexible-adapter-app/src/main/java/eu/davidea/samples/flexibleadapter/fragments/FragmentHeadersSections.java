package eu.davidea.samples.flexibleadapter.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.List;

import eu.davidea.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollGridLayoutManager;
import eu.davidea.flexibleadapter.items.IHeader;
import eu.davidea.flexibleadapter.items.ISectionable;
import eu.davidea.flipview.FlipView;
import eu.davidea.samples.flexibleadapter.ExampleAdapter;
import eu.davidea.samples.flexibleadapter.MainActivity;
import eu.davidea.samples.flexibleadapter.R;
import eu.davidea.samples.flexibleadapter.dialogs.BottomSheetDialog;
import eu.davidea.samples.flexibleadapter.dialogs.OnParameterSelectedListener;
import eu.davidea.samples.flexibleadapter.models.ExpandableHeaderItem;
import eu.davidea.samples.flexibleadapter.services.DatabaseConfiguration;
import eu.davidea.samples.flexibleadapter.services.DatabaseService;
import eu.davidea.utils.Utils;

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class FragmentHeadersSections extends AbstractFragment
		implements OnParameterSelectedListener {

	public static final String TAG = FragmentHeadersSections.class.getSimpleName();

	/**
	 * Custom implementation of FlexibleAdapter
	 */
	private ExampleAdapter mAdapter;


	public static FragmentHeadersSections newInstance(int columnCount) {
		FragmentHeadersSections fragment = new FragmentHeadersSections();
		Bundle args = new Bundle();
		args.putInt(ARG_COLUMN_COUNT, columnCount);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public FragmentHeadersSections() {
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//Settings for FlipView
		FlipView.resetLayoutAnimationDelay(true, 1000L);

		//Create New Database and Initialize RecyclerView
		DatabaseService.getInstance().createHeadersSectionsDatabase(400, 100);
		initializeRecyclerView(savedInstanceState);

		//Restore FAB button and icon
		initializeFab();

		//Settings for FlipView
		FlipView.stopLayoutAnimation();
	}

	@SuppressWarnings({"ConstantConditions", "NullableProblems"})
	private void initializeRecyclerView(Bundle savedInstanceState) {
		//Initialize Adapter and RecyclerView
		//ExampleAdapter makes use of stableIds, I strongly suggest to implement 'item.hashCode()'
		mAdapter = new ExampleAdapter(DatabaseService.getInstance().getDatabaseList(), getActivity());
		//Experimenting NEW features (v5.0.0)
		mAdapter.setRemoveOrphanHeaders(false)
				.setNotifyChangeOfUnfilteredItems(true)//We have highlighted text while filtering, so let's enable this feature to be consistent with the active filter
				.setAnimationOnScrolling(DatabaseConfiguration.animateOnScrolling);
		mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);
		mRecyclerView.setLayoutManager(createNewLinearLayoutManager());
		mRecyclerView.setAdapter(mAdapter);
		mRecyclerView.setHasFixedSize(true); //Size of RV will not change
		//NOTE: Use default item animator 'canReuseUpdatedViewHolder()' will return true if
		// a Payload is provided. FlexibleAdapter is actually sending Payloads onItemChange.
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		//Add FastScroll to the RecyclerView, after the Adapter has been attached the RecyclerView!!!
		mAdapter.setFastScroller((FastScroller) getActivity().findViewById(R.id.fast_scroller),
				Utils.getColorAccent(getActivity()), (MainActivity) getActivity());
		mAdapter.setLongPressDragEnabled(true)
				.setHandleDragEnabled(true)
				.setSwipeEnabled(true)
				.setUnlinkAllItemsOnRemoveHeaders(true)
				//Show Headers at startUp, 1st call, correctly executed, no warning log message!
				.setDisplayHeadersAtStartUp(true)
				.enableStickyHeaders()
				//Simulate developer 2nd call mistake, now it's safe, not executed, no warning log message!
				.setDisplayHeadersAtStartUp(true)
				//Simulate developer 3rd call mistake, still safe, not executed, warning log message displayed!
				.showAllHeaders();

		SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setEnabled(true);
		mListener.onFragmentChange(swipeRefreshLayout, mRecyclerView, SelectableAdapter.MODE_IDLE);

		//Add sample HeaderView items on the top (not belongs to the library)
		//mAdapter.addUserLearnedSelection(savedInstanceState == null);
		mAdapter.showLayoutInfo(savedInstanceState == null);
	}

	@Override
	public void performFabAction() {
		BottomSheetDialog bottomSheetDialogFragment = BottomSheetDialog.newInstance(R.layout.bottom_sheet_headers_sections, this);
		bottomSheetDialogFragment.show(getActivity().getSupportFragmentManager(), BottomSheetDialog.TAG);
	}

	@Override
	public void showNewLayoutInfo(MenuItem item) {
		super.showNewLayoutInfo(item);
		mAdapter.showLayoutInfo(true);
	}

	@Override
	public void onParameterSelected(int itemType, int referencePosition, int childPosition) {
		if (referencePosition < 0) return;
		int scrollTo, id;
		IHeader referenceHeader = getReferenceList().get(referencePosition);
		Log.d(TAG, "Adding New Item: ItemType=" + itemType +
				" referencePosition=" + referencePosition +
				" childPosition=" + childPosition);
		switch (itemType) {
			case 1: //Expandable
				id = mAdapter.getItemCountOfTypes(R.layout.recycler_expandable_item) + 1;
				ISectionable sectionableExpandable = DatabaseService.newExpandableItem(id, referenceHeader);
				mAdapter.addItemToSection(sectionableExpandable, referenceHeader, childPosition);
				scrollTo = mAdapter.getGlobalPositionOf(referenceHeader);
				break;
			case 2: //Expandable Header
				id = mAdapter.getItemCountOfTypes(R.layout.recycler_expandable_header_item) + 1;
				ExpandableHeaderItem expandableHeader = DatabaseService.newExpandableSectionItem(id);
				expandableHeader.setExpanded(mAdapter, false);
				mAdapter.addSection(expandableHeader, referenceHeader);
				scrollTo = mAdapter.getGlobalPositionOf(expandableHeader);
				break;
			case 3: //Header
				id = mAdapter.getItemCountOfTypes(R.layout.recycler_header_item) + 1;
				IHeader header = DatabaseService.newHeader(id);
				mAdapter.addSection(header, referenceHeader);
				scrollTo = mAdapter.getGlobalPositionOf(header);
				break;
			default: //case 0 = Simple Item
				id = mAdapter.getItemCountOfTypes(R.layout.recycler_expandable_item) + 1;
				ISectionable sectionable = DatabaseService.newSimpleItem(id, referenceHeader);
				mAdapter.addItemToSection(sectionable, referenceHeader, childPosition);
				scrollTo = mAdapter.getGlobalPositionOf(referenceHeader);
		}

		//With Sticky Headers enabled, this seems necessary to give
		// time at the RV to be in correct state before scrolling
		final int scrollToFinal = scrollTo;
		mRecyclerView.post(new Runnable() {
			@Override
			public void run() {
				mRecyclerView.smoothScrollToPosition(scrollToFinal);
			}
		});
	}

	@Override
	public List<IHeader> getReferenceList() {
		return mAdapter.getHeaderItems();
	}

	@Override
	protected GridLayoutManager createNewGridLayoutManager() {
		GridLayoutManager gridLayoutManager = new SmoothScrollGridLayoutManager(getActivity(), mColumnCount);
		gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				//NOTE: If you use simple integer to identify the ViewType,
				//here, you should use them and not Layout integers
				switch (mAdapter.getItemViewType(position)) {
					case R.layout.recycler_layout_item:
					case R.layout.recycler_uls_item:
					case R.layout.recycler_header_item:
					case R.layout.recycler_expandable_header_item:
						return mColumnCount;
					default:
						return 1;
				}
			}
		});
		return gridLayoutManager;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		Log.v(TAG, "onCreateOptionsMenu called!");
		inflater.inflate(R.menu.menu_sections, menu);
		mListener.initSearchView(menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.action_auto_collapse).setVisible(false);
		menu.findItem(R.id.action_expand_collapse_all).setVisible(false);
	}

}