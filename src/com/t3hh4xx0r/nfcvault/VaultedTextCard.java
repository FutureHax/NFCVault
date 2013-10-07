package com.t3hh4xx0r.nfcvault;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fima.cardsui.objects.Card;

public class VaultedTextCard extends Card {
	VaultedText password;
	OnViewButtonLister listener;
	public TextView desc;
	TextView title;
	ImageView view;
	ImageView typeIndicator;

	boolean showingTrueData = false;

	public boolean isShowingTrueData() {
		return showingTrueData;
	}

	public Context getContext() {
		return view.getContext();
	}

	public void setShowingTrueData(boolean showingTrueData) {
		this.showingTrueData = showingTrueData;
		if (showingTrueData) {
			view.setImageResource(R.drawable.ic_action_view);
		} else {
			view.setImageResource(R.drawable.ic_action_view_disabled);
			desc.setText(getPassword().dataValue);
		}
	}

	public VaultedTextCard(VaultedText pass, OnViewButtonLister listener) {
		super(pass.getDataTitle(), pass.getDataValue(), String.format("#%06X",
				(0xFFFFFF & ((int) 522))), String.format("#%06X",
				(0xFFFFFF & ((int) 522))), false, true);
		password = pass;
		this.listener = listener;
	}

	public VaultedText getPassword() {
		return password;
	}

	@Override
	public View getCardContent(Context context) {
		View v = LayoutInflater.from(context).inflate(R.layout.password_card,
				null);
		desc = (TextView) v.findViewById(R.id.description);
		typeIndicator = (ImageView) v.findViewById(R.id.type_indicator);
		if (getPassword().dataType.equals(VaultedText.TYPE_FILE)) {
			typeIndicator.setImageResource(R.drawable.ic_action_new_file);
		} else if (getPassword().dataType.equals(VaultedText.TYPE_IMAGE)) {
			typeIndicator.setImageResource(R.drawable.ic_action_new_picture);
		} else {
			typeIndicator.setImageResource(R.drawable.ic_action_abc);
		}
		view = (ImageView) v.findViewById(R.id.icon);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onViewButtonClicked(VaultedTextCard.this);
			}
		});
		title = (TextView) v.findViewById(R.id.title);
		title.setText(titlePlay);
		title.setTextColor(Color.parseColor(titleColor));
		desc.setText(description);

		if (isClickable == true) {
			((LinearLayout) v.findViewById(R.id.contentLayout))
					.setBackgroundResource(R.drawable.selectable_background_cardbank);
		}

		if (hasOverflow == true) {
			((ImageView) v.findViewById(R.id.overflow))
					.setVisibility(View.VISIBLE);
		} else {
			((ImageView) v.findViewById(R.id.overflow))
					.setVisibility(View.GONE);
		}
		return v;
	}

	public interface OnViewButtonLister {
		public void onViewButtonClicked(VaultedTextCard card);
	}
}
