package e.forms;

import java.awt.*;

public class FormBuilder {
    FormPanel formPanel;
    FormDialog formDialog;
    
    public FormBuilder(Frame parent, String title) {
        this.formPanel = new FormPanel();
        this.formDialog = new FormDialog(parent, title, formPanel);
    }
    
    public FormDialog getFormDialog() {
        return formDialog;
    }
    
    public FormPanel getFormPanel() {
        return formPanel;
    }
    
    public boolean show(String actionLabel) {
        return formDialog.show(actionLabel);
    }
    
    public void showNonModal() {
        formDialog.showNonModal();
    }
}
