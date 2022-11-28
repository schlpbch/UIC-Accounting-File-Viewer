package Accounting.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import Accounting.AccountingFactory;
import Accounting.AccountingPackage;
import Accounting.AccountingViewerData;
import Accounting.Carrier;
import Accounting.Carriers;
import Accounting.nls.NationalLanguageSupport;
import Accounting.presentation.AccountingEditor;
import Accounting.presentation.AccountingEditorPlugin;
import Accounting.utils.AccountingUtils;



public class ImportCarriersAction extends ImportCsvDataAction {


	public ImportCarriersAction(IEditingDomainProvider editingDomainProvider) {
		super(NationalLanguageSupport.ImportCarriersAction_0, editingDomainProvider);
		this.setToolTipText(this.getText());
		setImageDescriptor(AccountingUtils.getImageDescriptor("/icons/importCarriers24.png")); //$NON-NLS-1$
	}
	
	
	protected void run (IStructuredSelection structuredSelection) {
		
		final AccountingViewerData tool = AccountingUtils.getAccounting();
		
		final AccountingEditor editor = AccountingUtils.getActiveEditor(); 
		
		final EditingDomain domain = AccountingUtils.getActiveDomain();
		if (domain == null) return;
		
		if (tool == null) {
			MessageBox dialog =  new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
			dialog.setText(NationalLanguageSupport.ImportCarriersAction_1);
			dialog.open(); 
			return;
		}
		
		final BufferedReader br = super.getReader(NationalLanguageSupport.ImportCarriersAction_4, StandardCharsets.UTF_8.name());


		if (br == null) return;
		
		IRunnableWithProgress operation =	new IRunnableWithProgress() {
			// This is the method that gets invoked when the operation runs.

			public void run(IProgressMonitor monitor) {
				
				try {
					
					monitor.beginTask(NationalLanguageSupport.ImportCarriersAction_5, 4000); 
					
					AccountingUtils.addWorkflowStep("Import of Carriers started", editor);
		
					monitor.subTask(NationalLanguageSupport.ImportCarriersAction_6);
					prepareStructure(tool, domain);
					monitor.worked(10);

					monitor.subTask(NationalLanguageSupport.ImportCarriersAction_7);
			    	Carriers newCarriers = AccountingFactory.eINSTANCE.createCarriers();

			        String st; 
			        boolean isFirstLine = true;
					CompoundCommand command =  new CompoundCommand();
					
					while ((st = br.readLine()) != null) {
							
							Carrier carrier = decodeLine(st);
							
							if (!isFirstLine) {
								if (carrier != null) {
									newCarriers.getCarriers().add(carrier);	
								}
							} else {
								isFirstLine = false;
							}
							monitor.worked(1);
						}

			        
			        int added = 0;
			        int updated = 0;

			        
					monitor.subTask(NationalLanguageSupport.ImportCarriersAction_8);
					for (Carrier newCarrier : newCarriers.getCarriers()) {
			       	
						
			        	Carrier carrier = AccountingUtils.getCarrier(tool, newCarrier.getCode());
			        	
			        	if (carrier == null) {
			        		Command cmd2 = new AddCommand(domain, tool.getCodeLists().getCarriers().getCarriers(), newCarrier);
			        		command.append(cmd2);
			        		added++;
			        	} else {
			        		Command cmd2 = new SetCommand(domain, carrier, AccountingPackage.Literals.CARRIER__NAME, newCarrier.getName());
			                command.append(cmd2);
			        		Command cmd3 = new SetCommand(domain, carrier, AccountingPackage.Literals.CARRIER__SHORT_NAME, newCarrier.getShortName());
			                command.append(cmd3); 
			                updated++;
			           	}
			        			
			        }
			        
			        if (command != null && !command.isEmpty()) {
			        	domain.getCommandStack().execute(command);
						AccountingUtils.writeConsoleInfo(NationalLanguageSupport.ImportCarriersAction_9 + Integer.toString(added)+")" , editor); //$NON-NLS-2$
						AccountingUtils.writeConsoleInfo(NationalLanguageSupport.ImportCarriersAction_11 + Integer.toString(updated) + ")" , editor); //$NON-NLS-2$
			        }	
					monitor.worked(10);
					
					AccountingUtils.addWorkflowStep("Import of Carriers completed", editor);

					monitor.done();
					
				} catch (IOException e) {
					AccountingUtils.addWorkflowStep("Import of Carriers abandoned", editor);
					AccountingUtils.writeConsoleStackTrace(e, editor);
					return;			
				} finally {
					monitor.done();
				}
			}
		};
		try {
			// This runs the operation, and shows progress.
			editor.disconnectViews();
			new ProgressMonitorDialog(editor.getSite().getShell()).run(true, false, operation);
		} catch (Exception exception) {
			MessageBox dialog =  new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
			dialog.setText(NationalLanguageSupport.ImportCarriersAction_13);
			dialog.setMessage(exception.getMessage());
			dialog.open(); 
			AccountingEditorPlugin.INSTANCE.log(exception);
		} finally {
		editor.reconnectViews();
		}

		
	}



	private Carrier decodeLine(String st) {
		String[] strings = splitCsv(st);
		if (strings.length < 3) return null;
		
		if (strings[0].length() == 4) {
			Carrier carrier = AccountingFactory.eINSTANCE.createCarrier();
			carrier.setCode(strings[0]);
			
			String nameUTF8 = null;
			nameUTF8 = strings[2];			
			
			carrier.setName(nameUTF8);
			carrier.setShortName(strings[1]);
			return carrier;
		}
		return null;

	}

}
