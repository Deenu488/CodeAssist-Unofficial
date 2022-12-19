package com.tyron.code.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import com.tyron.code.R;
import com.google.android.material.transition.MaterialSharedAxis;
import android.content.DialogInterface;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.tyron.common.SharedPreferenceKeys;
import com.tyron.code.databinding.BaseTextinputLayoutBinding;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;
import java.io.File;
import android.widget.EditText;
import java.util.Objects;
import androidx.appcompat.app.AlertDialog;
import android.widget.Button;
import android.view.View;

public class GitSettingsFragment extends PreferenceFragmentCompat {
	private BaseTextinputLayoutBinding  binding;
	
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
		setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.git_preferences, rootKey);

			}
			

	@Override
	public boolean onPreferenceTreeClick(Preference preference) {

		String key = preference.getKey();
		SharedPreferences pref = preference.getSharedPreferences();
		String user_name = pref.getString("user_name", "");
		String user_email = pref.getString("user_email", "");

		binding = BaseTextinputLayoutBinding.inflate(getLayoutInflater());
		View view = binding.getRoot();

		switch (key) {
			case SharedPreferenceKeys.GIT_USER_NAME:

				AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(view)
					.setTitle(R.string.enter_full_name).setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok, null).create();

				dialog.setOnShowListener(d -> {

					final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

					binding.textinputLayout.setHint(R.string.git_user_name_title);
					EditText editText = binding.textinputLayout.getEditText();
					editText.setText(user_name);

					button.setOnClickListener(v -> {

						String name = binding.textinputLayout.getEditText().getText().toString();
						pref.edit().putString(SharedPreferenceKeys.GIT_USER_NAME, name).apply();
						preference.callChangeListener(name);
						d.dismiss();

					});
				});

				dialog.show();

				return true;

			case SharedPreferenceKeys.GIT_USER_EMAIL:

				AlertDialog dialog1 = new MaterialAlertDialogBuilder(requireContext()).setView(view)
					.setTitle(R.string.enter_email).setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok, null).create();

				dialog1.setOnShowListener(d -> {

					final Button button = dialog1.getButton(DialogInterface.BUTTON_POSITIVE);

					binding.textinputLayout.setHint(R.string.git_user_email_title);
					EditText editText = binding.textinputLayout.getEditText();
					editText.setText(user_email);

					button.setOnClickListener(v -> {

						String email = binding.textinputLayout.getEditText().getText().toString();
						pref.edit().putString(SharedPreferenceKeys.GIT_USER_EMAIL, email).apply();
						preference.callChangeListener(email);
						d.dismiss();

					});
				});

				dialog1.show();

				return true;
		}

		return super.onPreferenceTreeClick(preference);
	}
}

