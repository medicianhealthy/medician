# Medications List Implementation Plan

Refactor the Gallery screen into a modern Medications List using Material Design 3 (M3). The list will load data from `SharedPreferences`, support expandable items, and provide an option to edit medications.

## Proposed Changes

### [Medication Entity]

#### [Medication.java](file:///C:/Work/medician/app/src/main/java/com/robinzon/medicationwizard/entities/Medication.java)
- Add `public static final String SPK_MEDICATION_LIST` for global access.
- Implement `static Medication fromJson(JSONObject json)` to deserialize from `SharedPreferences`.
- Implement `static ArrayList<Medication> getSavedMedications(Context context)` helper.
- Improve `toJson` to store `hour` and `minute` separately for `SimpleDayTime` for better robustness.

#### [SimpleDayTime.java](file:///C:/Work/medician/app/src/main/java/com/robinzon/medicationwizard/utils/SimpleDayTime.java)
- Add helper to parse from JSON (hour/minute).

---

### [UI Refactoring]

#### [MedicationsListFragment.java](file:///C:/Work/medician/app/src/main/java/com/robinzon/medicationwizard/ui/medicationslist/MedicationsListFragment.java) (Renamed from GalleryFragment)
- Implement `RecyclerView` setup.
- Load medications from `ViewModel`.

#### [MedicationsListViewModel.java](file:///C:/Work/medician/app/src/main/java/com/robinzon/medicationwizard/ui/medicationslist/MedicationsListViewModel.java) (Renamed from GalleryViewModel)
- Add `LiveData<List<Medication>>` to observe the medication list.
- Method to refresh data from `SharedPreferences`.

#### [MedicationsListAdapter.java](file:///C:/Work/medician/app/src/main/java/com/robinzon/medicationwizard/ui/medicationslist/MedicationsListAdapter.java) [NEW]
- Implement M3 expandable card logic.
- Collapsed: Name, Strength, Form Icon.
- Expanded: All details (Frequency, Instructions, Times) and "Edit" button.

---

### [Resources & Navigation]

#### [fragment_medications_list.xml](file:///C:/Work/medician/app/src/main/res/layout/fragment_medications_list.xml) (Renamed from fragment_gallery.xml)
- Add `RecyclerView`.
- Use `androidx.swiperefreshlayout.widget.SwipeRefreshLayout` for a modern feel.

#### [item_medication_list.xml](file:///C:/Work/medician/app/src/main/res/layout/item_medication_list.xml) [NEW]
- Design the M3 card with expandable content.

#### [mobile_navigation.xml](file:///C:/Work/medician/app/src/main/res/navigation/mobile_navigation.xml)
- Update fragment name and label.

---

## Verification Plan

### Automated Tests
- N/A (Manual UI verification preferred for this refactoring).

### Manual Verification
1. Add a few medications via the "Add" FAB.
2. Navigate to "Medications List".
3. Verify all added meds are displayed alphabetically (using the existing `Comparable` implementation).
4. Tap an item to expand/collapse.
5. Verify expanded details match the input.
6. Tap "Edit" (Future step: ensure it opens the bottom sheet with pre-filled data).
