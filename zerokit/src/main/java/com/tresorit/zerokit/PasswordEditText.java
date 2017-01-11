package com.tresorit.zerokit;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.util.Arrays;

public final class PasswordEditText extends AppCompatEditText implements PasswordHandler {

    private final LongSparseArray<Editable> editablePool = new LongSparseArray<>();

    public PasswordEditText(Context context) {
        super(context);
        init();
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        super.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        super.setText("");
    }

    public final void setPasswordExporter(PasswordExporter passwordExporter) {
        passwordExporter.setEditText(this);
    }

    public final PasswordExporter getPasswordExporter() {
        return new PasswordExporter(this);
    }

    @Override
    public final Editable getText() {
        if (editablePool == null) return super.getText();
        int length = super.getText().length();
        @SuppressWarnings("ConstantConditions")
        Editable result = editablePool == null ? super.getText() : editablePool.get(length);
        if (result == null) {
            char[] chars = new char[length];
            Arrays.fill(chars, '*');
            result = Editable.Factory.getInstance().newEditable(new String(chars));
            editablePool.put(length, result);
        }
        return result;
    }

    @Override
    public final void setText(CharSequence text, BufferType type) {
        super.setText("", type);
    }

    @Override
    public final void setInputType(int type) {
    }


    @Override
    public final Parcelable onSaveInstanceState() {
        clear();
        return super.onSaveInstanceState();
    }

    @Override
    public final void addTextChangedListener(TextWatcher watcher) {
    }


    //**********************************

    @Override
    public final void clear() {
        super.getText().clear();
    }

    @Override
    public final int length() {
        return super.getText().length();
    }

    @Override
    public final boolean isEmpty() {
        return length() <= 0;
    }

    @Override
    public final boolean isContentEqual(PasswordEditText passwordEditText) {
        char[] charArray1 = getCharArray(false);
        char[] charArray2 = passwordEditText.getCharArray(false);
        boolean result = Arrays.equals(charArray1, charArray2);
        Arrays.fill(charArray1, '\u0000');
        Arrays.fill(charArray2, '\u0000');
        return result;
    }

    @Override
    public final boolean isContentEqual(PasswordExporter exporter) {
        return isContentEqual(exporter.editText);
    }

    final char[] getCharArray(boolean clear) {
        Editable editable = super.getText();
        char[] chars = new char[editable.length()];
        editable.getChars(0, chars.length, chars, 0);
        if (clear) editable.clear();
        return chars;
    }

    public final static class PasswordExporter implements PasswordHandler {

        PasswordEditText editText;

        public PasswordExporter() {
        }

        PasswordExporter(PasswordEditText editText) {
            this.editText = editText;
        }

        @Override
        public final int length() {
            return editText.length();
        }

        @Override
        public final boolean isEmpty() {
            return editText.isEmpty();
        }

        @Override
        public void clear() {
            editText.clear();
        }

        @Override
        public final boolean isContentEqual(PasswordExporter passwordExporter) {
            return editText.isContentEqual(passwordExporter);
        }

        @Override
        public final boolean isContentEqual(PasswordEditText editText) {
            return editText.isContentEqual(editText);
        }

        final void setEditText(PasswordEditText editText) {
            this.editText = editText;
        }

        final char[] getCharArray(boolean clear) {
            return editText.getCharArray(clear);
        }
    }

}
