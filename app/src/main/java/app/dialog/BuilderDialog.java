package app.dialog;

import androidx.annotation.Keep;

import java.util.HashMap;

@Keep
public abstract class BuilderDialog<T> {

    protected String title;
    protected String content;
    protected String positiveButton;
    protected String negativeButton;
    protected boolean cancelable = true;
    protected boolean canOntouchOutside = true;

    protected DialogActionListener.PositiveButtonListener<T> positiveButtonListener;
    protected DialogActionListener.SetNegativeButtonListener<T> negativeButtonListener;
    protected DialogActionListener.DismissDialogListener dismissDialogListener;

    public BuilderDialog<T> setTitle(String title) {
        this.title = title;
        return this;
    }

    public BuilderDialog<T> setContent(String content) {
        this.content = content;
        return this;
    }

    public BuilderDialog<T> setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        return this;
    }

    public BuilderDialog<T> setCanOntouchOutside(boolean canOntouchOutside) {
        this.canOntouchOutside = canOntouchOutside;
        return this;
    }

    public BuilderDialog<T> onSetPositiveButton(String positiveButton, DialogActionListener.PositiveButtonListener<T> positiveButtonListener) {
        this.positiveButton = positiveButton;
        this.positiveButtonListener = positiveButtonListener;
        return this;
    }

    public BuilderDialog<T> onSetNegativeButton(String negativeButton, DialogActionListener.SetNegativeButtonListener<T> negativeButtonListener) {
        this.negativeButton = negativeButton;
        this.negativeButtonListener = negativeButtonListener;
        return this;
    }

    public BuilderDialog<T> onDismissListener(DialogActionListener.DismissDialogListener dismissDialogListener) {
        this.dismissDialogListener = dismissDialogListener;
        return this;
    }

    public abstract T build();

    public interface DialogActionListener {

        interface PositiveButtonListener<T> {
            void onPositiveButtonListener(T baseDialog, HashMap<String, Object> datas);
        }

        interface SetNegativeButtonListener<T> {
            void onNegativeButtonListener(T baseDialog);
        }

        interface DismissDialogListener {
            void onDismissDialogListner();
        }

    }

}
