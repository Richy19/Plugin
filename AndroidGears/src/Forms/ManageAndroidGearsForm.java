package Forms;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import Models.GearSpec.GearSpecDependency;
import Models.GearSpec.GearSpecUpdate;
import Panels.SpecDetailsPanel;
import Renderers.GearSpecCellRenderer;
import Renderers.ModuleCellRenderer;
import Renderers.ProjectCellRenderer;
import Singletons.SettingsManager;
import Utilities.Utils;

import java.net.URI;
import java.util.*;

import Models.GearSpec.GearSpec;
import Workers.*;
import Workers.Git.GitWorker;
import Workers.InstallUninstall.*;
import Workers.Search.SearchInstalledProjectsWorker;
import Workers.Search.SearchUpdatableProjectsWorker;
import Workers.Search.SearchDeclaredDependenciesWorker;
import Workers.Search.SearchProjectListWorker;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jdesktop.swingx.combobox.ListComboBoxModel;

/**
 * Created by matthewyork on 4/1/14.
 */
public class ManageAndroidGearsForm{
    public static final int DETAILS_INNER_WIDTH = 230;
    private static final int AGREE_TO_UNINSTALL_GEAR = 1;
    private static final int AGREE_TO_UNINSTALL_GEAR_AND_DEPENDENTS = 2;

    File androidGearsDirectory;
    private GearSpec selectedSpec;
    private GearSpecUpdate selectedUpdateSpec;
    private ArrayList<GearSpec> availableGears;
    private ArrayList<GearSpec> declaredProjects;
    private ArrayList<GearSpec> installedProjects;
    private ArrayList<GearSpecUpdate> updatableProjects;
    private ArrayList<String> projectVersions;
    Project[] targetProjects;
    Module[] targetModules;

    private JTextField SearchTextField;
    private JTabbedPane SearchTabbedPane;
    private JButton doneButton;
    public JPanel MasterPanel;
    private JPanel SearchPanel;
    private JPanel DetailsPanel;
    private JList AllGearsList;
    private JList InstalledList;
    private JScrollPane DetailsScrollPane;
    private JButton SyncButton;
    private JLabel StatusLabel;
    private JList VersionsList;
    private JLabel ChangeVersionsLabel;
    private JButton InstallUninstallButton;
    private JButton OpenInBrowserButton;
    private JLabel LoadingSpinnerLabel;
    private JComboBox TargetProjectComboBox;
    private JLabel HeaderLogo;
    private JList DeclaredList;
    private JButton DeclareUndeclareGearButton;
    private JComboBox TargetModuleComboBox;
    private JList UpdatesList;
    private JPanel UpdatesTabPanel;
    private JButton UpdateGearButton;

    private void createUIComponents() {

    }

    public ManageAndroidGearsForm() {
        setupComboBoxes();
        setupMiscUI();
        setupTables();
        setupSearchTextField();
        setupButtons();
    }

    private void setupTables() {

        //Add directories mode
        SearchProjectListWorker worker = new SearchProjectListWorker("", targetProjects[TargetProjectComboBox.getSelectedIndex()]){
            @Override
            protected void done() {
                super.done();
                availableGears = this.specs;
            }
        };
        worker.execute();

        //Get declared gears
        refreshDeclaredList("");

        //Get installed gears
        refreshInstalledList("");

        //Get updatable gears
        refreshUpdatedList("");

        //Setup click listener
        AllGearsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                didSelectSearchSpecAtIndex(AllGearsList.getSelectedIndex());
            }
        });
        AllGearsList.setFocusable(false);

        DeclaredList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                didSelectDeclaredSpecAtIndex(DeclaredList.getSelectedIndex());
            }
        });
        DeclaredList.setFocusable(false);

        InstalledList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                didSelectInstalledSpecAtIndex(InstalledList.getSelectedIndex());
            }
        });
        InstalledList.setFocusable(false);

        UpdatesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                didSelectUpdatesSpecAtIndex(UpdatesList.getSelectedIndex());
            }
        });
        UpdatesList.setFocusable(false);

        VersionsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                didSelectSpecVersion(VersionsList.getSelectedIndex());
            }
        });
        VersionsList.setFocusable(false);
    }

    private void setupSearchTextField() {
        SearchTextField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {

            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                //Get pressed character
                char c = keyEvent.getKeyChar();

                //Build searchString
                String searchString = SearchTextField.getText();
                if(c == 8 && searchString.length() > 0){
                    searchString = SearchTextField.getText().substring(0, searchString.length()-1);
                }
                else if(isValidCharacter(c)){
                    searchString = SearchTextField.getText()+keyEvent.getKeyChar();
                }


                //Switch to desired tab
                refreshSelectedTabList(searchString);
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });
    }

    private void setupButtons(){
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFrame frame  = (JFrame)SwingUtilities.getWindowAncestor(MasterPanel);
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });

        SyncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //Set synchronizing
                StatusLabel.setText("Synchronizing available gears with server...");
                LoadingSpinnerLabel.setVisible(true);

                //Synchronize Specs
                GitWorker worker = new GitWorker(){
                    @Override
                    protected void done() {
                        super.done();
                        StatusLabel.setText("Gears successfully synced with server");
                        LoadingSpinnerLabel.setVisible(false);
                    }
                };
                worker.execute();
            }
        });

        //Declare/Undeclare button
        DeclareUndeclareGearButton.setVisible(false);
        DeclareUndeclareGearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                toggleDependencyDeclaration();
            }
        });

        //Update button
        UpdateGearButton.setVisible(false);
        UpdateGearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateGear(selectedUpdateSpec);
            }
        });
        UpdateGearButton.setVisible(false);

        //Install/Uninstall button
        InstallUninstallButton.setVisible(false);
        InstallUninstallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (SearchTabbedPane.getSelectedIndex() == 3){
                    toggleDependency(selectedUpdateSpec);
                }
                else {
                    toggleDependency(selectedSpec);
                }

            }
        });

        //Show homepage button
        OpenInBrowserButton.setVisible(false);

        //Show in browser
        OpenInBrowserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openSpecHomePageInBrowser();
            }
        });
    }

    private void setupComboBoxes(){
        //Get all projects
        ProjectManager pm = ProjectManager.getInstance();
        targetProjects = pm.getOpenProjects();
        Project p = targetProjects[0];
        SettingsManager.getInstance().loadProjectSettings(p);

        //Get all modules
        ModuleManager mm = ModuleManager.getInstance(p);
        targetModules = mm.getModules();

        //Setup Project Combo Box
        TargetProjectComboBox.setModel(new ListComboBoxModel<Project>(Arrays.asList(targetProjects)));
        TargetProjectComboBox.setSelectedIndex(0);
        TargetProjectComboBox.setRenderer(new ProjectCellRenderer());
        TargetProjectComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        });
        TargetProjectComboBox.setFocusable(false);

        //Setup Module Combo Box
        TargetModuleComboBox.setModel(new ListComboBoxModel<Module>(Arrays.asList(targetModules)));
        if (targetModules.length > 0) {
            TargetModuleComboBox.setSelectedIndex(targetModules.length-1);
        }
        TargetModuleComboBox.setRenderer(new ModuleCellRenderer());
        TargetModuleComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Module module = (Module)TargetModuleComboBox.getSelectedItem();
                SettingsManager.getInstance().setMainModule(module.getName(), targetProjects[TargetProjectComboBox.getSelectedIndex()]);
            }
        });
        TargetModuleComboBox.setFocusable(false);

        //Get/Set selected module
        String mainModule = SettingsManager.getInstance().getMainModule();
        if (mainModule.equals("")){ //Save default
            SettingsManager.getInstance().setMainModule(targetModules[targetModules.length-1].getName(), p);
        }
        else { //Pull back selected module
            for (int ii = 0; ii < targetModules.length; ii++){
                if (targetModules[ii].getName().equals(mainModule)){
                    TargetModuleComboBox.setSelectedIndex(ii);
                }
            }
        }


    }

    private void setupMiscUI() {
        ChangeVersionsLabel.setFont(new Font(ChangeVersionsLabel.getFont().getName(), Font.PLAIN, 12));
        StatusLabel.setText("");
        LoadingSpinnerLabel.setVisible(false);

        //Focus search bar
        SearchTextField.setVisible(true);
        SearchTextField.requestFocusInWindow();

        //Set header logo background clear
        HeaderLogo.setOpaque(false);

        //Set up listener for change in tab state
        SearchTabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {

                //Switch to desired tab
                SearchTextField.setText("");
                refreshSelectedTabList(SearchTextField.getText());
                clearDetailsUI();
            }
        });
        SearchTabbedPane.setFocusable(false);
    }

    ///////////////////////
    // Table refresh/reload
    ////////////////////////

    private void refreshSelectedTabList(String searchString){
        switch (SearchTabbedPane.getSelectedIndex()){
            case 0: refreshAvailableGearsList(searchString);
                break;
            case 1: refreshDeclaredList(searchString);
                break;
            case 2: refreshInstalledList(searchString);
                break;
            case 3: refreshUpdatedList(searchString);
                break;
        }
    }

    private void reloadSearchList(){
        AllGearsList.setListData(availableGears.toArray());
        AllGearsList.setCellRenderer(new GearSpecCellRenderer());
        AllGearsList.setVisibleRowCount(availableGears.size());
    }

    private void reloadDeclaredList(){
        DeclaredList.setListData(declaredProjects.toArray());
        DeclaredList.setCellRenderer(new GearSpecCellRenderer());
        DeclaredList.setVisibleRowCount(declaredProjects.size());
    }

    private void reloadInstalledList(){
        InstalledList.setListData(installedProjects.toArray());
        InstalledList.setCellRenderer(new GearSpecCellRenderer());
        InstalledList.setVisibleRowCount(installedProjects.size());
    }

    private void reloadUpdatesList(){
        UpdatesList.setListData(updatableProjects.toArray());
        UpdatesList.setCellRenderer(new GearSpecCellRenderer());
        UpdatesList.setVisibleRowCount(updatableProjects.size());

        //Set number of available updates in the updates tab
        SearchTabbedPane.setTitleAt(3, "Updates ("+ updatableProjects.size()+")");
    }

    private void reloadVersionList() {
        VersionsList.setListData(projectVersions.toArray());
        VersionsList.setCellRenderer(new DefaultListCellRenderer());
        VersionsList.setVisibleRowCount(projectVersions.size());
    }

    private void refreshAvailableGearsList(String searchString){
        //Get availableGears and reload
        SearchProjectListWorker worker = new SearchProjectListWorker(searchString, targetProjects[TargetProjectComboBox.getSelectedIndex()]){
            @Override
            protected void done() {
                super.done();
                availableGears = this.specs;
                reloadSearchList();
            }
        };
        worker.execute();
    }

    private void refreshDeclaredList(final String searchString){
        SearchDeclaredDependenciesWorker declaredProjectsWorker = new SearchDeclaredDependenciesWorker(targetProjects[TargetProjectComboBox.getSelectedIndex()], searchString){

            @Override
            protected void done() {
                super.done();

                declaredProjects = this.specs;
                reloadDeclaredList();
            }
        };
        declaredProjectsWorker.execute();
    }

    private void refreshInstalledList(final String searchString){
        SearchInstalledProjectsWorker installedProjectsWorker = new SearchInstalledProjectsWorker(targetProjects[TargetProjectComboBox.getSelectedIndex()], searchString){

            @Override
            protected void done() {
                super.done();

                installedProjects = this.specs;
                reloadInstalledList();
            }
        };
        installedProjectsWorker.execute();
    }

    private void refreshUpdatedList(final String searchString){
        SearchUpdatableProjectsWorker installedProjectsWorker = new SearchUpdatableProjectsWorker(targetProjects[TargetProjectComboBox.getSelectedIndex()], searchString){

            @Override
            protected void done() {
                super.done();

                updatableProjects = this.specs;
                reloadUpdatesList();
            }
        };
        installedProjectsWorker.execute();
    }

    private Boolean isValidCharacter(char c){
        //Number
        if(c >= 32 && c <= 126){
            return true;
        }

        return false;
    }


    ///////////////////////
    // JList Selection
    ////////////////////////

    private void didSelectSearchSpecAtIndex(int index){
        if (index >= 0 && index < availableGears.size()){
            selectedSpec = availableGears.get(index);
            setDetailsForSpec(selectedSpec);
            getVersionDetailsForSpec();
        }
    }

    private void didSelectDeclaredSpecAtIndex(int index){
        if (index >= 0 && index < declaredProjects.size()){
            selectedSpec = declaredProjects.get(index);
            setDetailsForSpec(selectedSpec); //MAY NEED TO CHANGE
            getVersionDetailsForSpec();
        }

    }

    private void didSelectInstalledSpecAtIndex(int index){
        if (index >= 0 && index < installedProjects.size()){
            selectedSpec = installedProjects.get(index);
            setDetailsForSpec(selectedSpec); //MAY NEED TO CHANGE
            getVersionDetailsForSpec();
        }

    }

    private void didSelectUpdatesSpecAtIndex(int index){
        if (index >= 0 && index < updatableProjects.size()){
            selectedUpdateSpec = updatableProjects.get(index);
            setDetailsForSpec(selectedUpdateSpec);
            getVersionDetailsForSpec();
        }
    }

    private void didSelectSpecVersion(int index) {
        if (index >= 0 && index < projectVersions.size()){
            selectedSpec = Utils.specForInfo(selectedSpec.getName(), projectVersions.get(index));
            setDetailsForSpec(selectedSpec);
        }
    }

    ////////////////////////
    // Details Management
    ///////////////////////

    private void setDetailsForSpec(GearSpec spec){
        //If it is the same as you have selected, don't do anything, else, get the specified version

        SpecDetailsPanel specDetailsPanel = new SpecDetailsPanel(spec);

        //Set panel in scrollpane
        DetailsScrollPane.setViewportView(specDetailsPanel);
        DetailsScrollPane.revalidate();
        DetailsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DetailsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        //Set install/uninstall button
        InstallUninstallButton.setVisible(true);

        //Enable show homepage button again
        OpenInBrowserButton.setVisible(true);

        //Set declaration button based on install state
        setDeclarationStatusForSpec(spec);

        //Set update button
        if (spec instanceof GearSpecUpdate){
            UpdateGearButton.setText("Update Gear to "+((GearSpecUpdate) spec).getUpdateVersionNumber());
            UpdateGearButton.setVisible(true);
        }
        else {
            UpdateGearButton.setVisible(false);
        }
    }

    private void clearDetailsUI() {
        //Clear information
        SpecDetailsPanel specDetailsPanel = new SpecDetailsPanel(new GearSpec());

        //Set panel in scrollpane
        DetailsScrollPane.setViewportView(specDetailsPanel);
        DetailsScrollPane.revalidate();
        DetailsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DetailsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        //Clear version list entry
        projectVersions = new ArrayList<String>();
        reloadVersionList();

        //Hide buttons
        InstallUninstallButton.setVisible(false);
        UpdateGearButton.setVisible(false);
        OpenInBrowserButton.setVisible(false);
        DeclareUndeclareGearButton.setVisible(false);
    }

    private void setDeclarationStatusForSpec(final GearSpec spec){
        GetGearStateWorker worker = new GetGearStateWorker(targetProjects[TargetProjectComboBox.getSelectedIndex()], spec){
            @Override
            protected void done() {
                super.done();

                if (this.gearState == GearSpec.GearState.GearStateUninstalled){
                    DeclareUndeclareGearButton.setText("Declare Gear");
                    InstallUninstallButton.setText("Install Gear");
                    DeclareUndeclareGearButton.setVisible(true);

                }
                else if (this.gearState == GearSpec.GearState.GearStateDeclared){
                    DeclareUndeclareGearButton.setText("Undeclare Gear");
                    DeclareUndeclareGearButton.setVisible(true);
                    InstallUninstallButton.setText("Install Gear");
                }
                else if (this.gearState == GearSpec.GearState.GearStateInstalled){
                    DeclareUndeclareGearButton.setVisible(false);
                    InstallUninstallButton.setText("Uninstall Gear");
                }
            }
        };
          worker.execute();
    }

    private void getVersionDetailsForSpec(){
        //Set versions
        GetProjectVersionsWorker worker = new GetProjectVersionsWorker(selectedSpec){
            @Override
            protected void done() {
                super.done();

                projectVersions = this.versions;

                reloadVersionList();
            }
        };
        worker.execute();
    }

    ///////////////////////
    // Website loading
    ///////////////////////
    private void openSpecHomePageInBrowser(){
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                if (selectedSpec.getHomepage().contains("github.com")){
                    desktop.browse(URI.create(selectedSpec.getHomepage()+"/tree/"+selectedSpec.getSource().getTag()));
                }
                else {
                    desktop.browse(URI.create(selectedSpec.getHomepage()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ///////////////////////
    // Install / Uninstall
    ///////////////////////

    private void toggleDependency(final GearSpec spec){
        Project targetProject = targetProjects[TargetProjectComboBox.getSelectedIndex()];

        if (spec.getGearState() == GearSpec.GearState.GearStateInstalled){
            ArrayList<GearSpec> gearsToUninstall = new ArrayList<GearSpec>();
            gearsToUninstall.add(spec);

            //Prompt to add dependents
            ArrayList<GearSpec> dependents = spec.dependentGears(targetProject);
            if (dependents.size() > 0){
                gearsToUninstall.addAll(warnOfDependents(dependents));
            }

            //Prompt to add dependencies
            if (spec.getDependencies() != null){
                if (spec.getDependencies().size() > 0){
                    gearsToUninstall.addAll(warnOfDependencies(spec));
                }
            }

            uninstallDependencies(gearsToUninstall, targetProjects[TargetProjectComboBox.getSelectedIndex()], targetModules[TargetModuleComboBox.getSelectedIndex()]);
        }
        else {
            //Set UI in download state
            StatusLabel.setText("Installing Gear and its dependencies: " + spec.getName());
            LoadingSpinnerLabel.setVisible(true);
            InstallUninstallButton.setEnabled(false);
            DeclareUndeclareGearButton.setEnabled(false);
            SyncButton.setEnabled(false);


            InstallDependencyForSpecWorker worker = new InstallDependencyForSpecWorker(spec, targetProjects[TargetProjectComboBox.getSelectedIndex()], targetModules[TargetModuleComboBox.getSelectedIndex()]){

                @Override
                protected void done() {
                    super.done();

                    //Hide loading spinner and renable buttons
                    LoadingSpinnerLabel.setVisible(false);
                    InstallUninstallButton.setEnabled(true);
                    SyncButton.setEnabled(true);
                    DeclareUndeclareGearButton.setEnabled(true);
                    setDeclarationStatusForSpec(spec);

                    //Flip button text
                    if (this.successful){
                        DeclareUndeclareGearButton.setVisible(false);
                        InstallUninstallButton.setText("Uninstall Gear");
                        StatusLabel.setText("Successfully installed: "+spec.getName());

                        //Reload tables
                        refreshSelectedTabList(SearchTextField.getText());
                    }
                    else {
                        StatusLabel.setText("Installation failed for: " + spec.getName());
                    }
                }
            };
            worker.execute();
        }
    }

    private void toggleDependencyDeclaration(){
        DeclareUndeclareGearButton.setEnabled(false);
        InstallUninstallButton.setEnabled(false);

        if (this.selectedSpec.getGearState() == GearSpec.GearState.GearStateDeclared){
            UndeclareSpecWorker worker = new UndeclareSpecWorker(this.selectedSpec, targetProjects[TargetProjectComboBox.getSelectedIndex()]){
                @Override
                protected void done() {
                    super.done();

                    DeclareUndeclareGearButton.setEnabled(true);
                    InstallUninstallButton.setEnabled(true);

                    if (success){
                        StatusLabel.setText("Successfully undeclared: "+ManageAndroidGearsForm.this.selectedSpec.getName());
                        DeclareUndeclareGearButton.setText("Declare Gear");

                        //Set new declaration state on local copy of selected spec
                        ManageAndroidGearsForm.this.selectedSpec.setGearState(GearSpec.GearState.GearStateUninstalled);
                        setDetailsForSpec(ManageAndroidGearsForm.this.selectedSpec);

                        //Reload tables
                        refreshSelectedTabList(SearchTextField.getText());
                    }
                    else {
                        StatusLabel.setText("Failed to undeclare:: "+ManageAndroidGearsForm.this.selectedSpec.getName());
                    }
                }
            };
            worker.execute();
        }
        else {
            DeclareSpecWorker worker = new DeclareSpecWorker(this.selectedSpec, targetProjects[TargetProjectComboBox.getSelectedIndex()]){
                @Override
                protected void done() {
                    super.done();

                    DeclareUndeclareGearButton.setEnabled(true);
                    InstallUninstallButton.setEnabled(true);

                    if (success){
                        StatusLabel.setText("Successfully declared: "+ManageAndroidGearsForm.this.selectedSpec.getName());
                        DeclareUndeclareGearButton.setText("Undeclare Gear");

                        //Set new declaration state on local copy of selected spec
                        ManageAndroidGearsForm.this.selectedSpec.setGearState(GearSpec.GearState.GearStateDeclared);
                        setDetailsForSpec(ManageAndroidGearsForm.this.selectedSpec);

                        //Reload tables
                        refreshSelectedTabList(SearchTextField.getText());
                    }
                    else {
                        StatusLabel.setText("Failed to declare:: "+ManageAndroidGearsForm.this.selectedSpec.getName());
                    }
                }
            };
            worker.execute();
        }
    }

    private ArrayList<GearSpec> warnOfDependents(ArrayList<GearSpec> dependents){
        String dependentString = "";
        for(GearSpec dependentGear : dependents){
            dependentString= dependentString+dependentGear.getName()+ " - "+dependentGear.getVersion()+"\n";
        }

        String dependencyMessageString = "The gear you wish to uninstall has other gears depending on it:\n"+dependentString
                +"\nContinuing could cause unexpected behavior in these gears.";

        Object[] options = {"Cancel",
                "Continue", "Continue and Uninstall Dependents"};
        int answer = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(MasterPanel),
                dependencyMessageString,
                "Dependency check",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        //Process answer
        if (answer == AGREE_TO_UNINSTALL_GEAR){
            return new ArrayList<GearSpec>();
        }
        else if (answer == AGREE_TO_UNINSTALL_GEAR_AND_DEPENDENTS){
            return dependents;
        }
        else {
            return new ArrayList<GearSpec>();
        }
    }

    private ArrayList<GearSpec> warnOfDependencies(GearSpec spec){
        if (spec.getDependencies() != null){
            String dependentString = "";
            ArrayList<GearSpec> dependencies = new ArrayList<GearSpec>();
            for(GearSpecDependency dependency : spec.getDependencies()){
                dependentString= dependentString+dependency.getName()+ " - "+dependency.getVersion()+"\n";
                dependencies.add(Utils.specForInfo(dependency.getName(), dependency.getVersion()));
            }

            String dependencyMessageString = "The gear you wish to uninstall depends on other gears:\n"+dependentString
                    +"\nWould you also like to uninstall these gears?.";

            Object[] options = {"No",
                    "Yes"};
            int answer = JOptionPane.showOptionDialog(SwingUtilities.getWindowAncestor(MasterPanel),
                    dependencyMessageString,
                    "Dependency check",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            //Process answer
            if (answer == AGREE_TO_UNINSTALL_GEAR){
                return dependencies;
            }
        }

        return new ArrayList<GearSpec>();
    }

    private void uninstallDependencies(ArrayList<GearSpec> specs, final Project project, Module module){
        //Set UI in uninstall state
        StatusLabel.setText("Uninstalling gears");
        LoadingSpinnerLabel.setVisible(true);
        InstallUninstallButton.setEnabled(false);
        SyncButton.setEnabled(false);
        UpdateGearButton.setEnabled(false);

        UninstallDependencyForSpecWorker worker = new UninstallDependencyForSpecWorker(specs, project, module){

            @Override
            protected void done() {
                super.done();

                //Hide loading spinner and re-enable buttons
                LoadingSpinnerLabel.setVisible(false);
                InstallUninstallButton.setEnabled(true);
                SyncButton.setEnabled(true);
                UpdateGearButton.setEnabled(true);

                //Flip button text
                if (this.successful){
                    DeclareUndeclareGearButton.setVisible(true);
                    InstallUninstallButton.setText("Install Gear");
                    StatusLabel.setText("Successfully uninstalled gear.");
                    UpdateGearButton.setVisible(false);
                    refreshSelectedTabList(SearchTextField.getText());

                    //If on the updates page, hide everything
                    if (SearchTabbedPane.getSelectedIndex() >= 1){
                        clearDetailsUI();
                    }
                    else {
                        setDeclarationStatusForSpec(ManageAndroidGearsForm.this.selectedSpec);
                    }
                }
                else {
                    StatusLabel.setText("There was a problem uninstalling the gear. Please try again.");
                }
            }
        };
        worker.execute();
    }

    private void updateGear(GearSpecUpdate spec){
        //Set UI in uninstall state
        StatusLabel.setText("Updating "+spec.getName()+" to version "+spec.getUpdateVersionNumber());
        LoadingSpinnerLabel.setVisible(true);
        InstallUninstallButton.setEnabled(false);
        SyncButton.setEnabled(false);
        UpdateGearButton.setEnabled(false);

        //Update gear
        UpdateGearWorker worker = new UpdateGearWorker(spec, targetProjects[TargetProjectComboBox.getSelectedIndex()], targetModules[TargetModuleComboBox.getSelectedIndex()]){
            @Override
            protected void done() {
                super.done();

                //Re-enable UI elements
                LoadingSpinnerLabel.setVisible(false);
                InstallUninstallButton.setEnabled(true);
                UpdateGearButton.setEnabled(true);
                SyncButton.setEnabled(true);

                if (successful){
                    //Set status
                    StatusLabel.setText("Successfully updated gear.");

                    //Reload tables
                    refreshSelectedTabList(SearchTextField.getText());
                }
                else {
                    StatusLabel.setText("There was a problem updating the gear. Please try again.");
                }
            }
        };
        worker.execute();
    }
}


