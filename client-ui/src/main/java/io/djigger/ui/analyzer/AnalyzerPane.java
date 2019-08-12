/*******************************************************************************
 * (C) Copyright 2016 Jérôme Comte and Dorian Cransac
 *
 *  This file is part of djigger
 *
 *  djigger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  djigger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with djigger.  If not, see <http://www.gnu.org/licenses/>.
 *
 *******************************************************************************/
package io.djigger.ui.analyzer;

import io.djigger.aggregation.AnalyzerService;
import io.djigger.aggregation.filter.BranchFilterFactory;
import io.djigger.aggregation.filter.NodeFilterFactory;
import io.djigger.monitoring.java.instrumentation.subscription.RealNodePathSubscription;
import io.djigger.monitoring.java.instrumentation.subscription.SimpleSubscription;
import io.djigger.monitoring.java.model.StackTraceElement;
import io.djigger.ql.Filter;
import io.djigger.ql.OQLFilterBuilder;
import io.djigger.ui.Session;
import io.djigger.ui.common.EnhancedTextField;
import io.djigger.ui.common.NodePresentationHelper;
import io.djigger.ui.model.AnalysisNode;
import io.djigger.ui.model.NodeID;
import io.djigger.ui.model.RealNodePath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;


public abstract class AnalyzerPane extends Dashlet implements ActionListener {

    private static final long serialVersionUID = -8452799701138552693L;

    protected final Session main;

    protected final AnalyzerGroupPane parent;

    private Filter<NodeID> nodeFilter;

    protected AnalysisNode workNode;

    private EnhancedTextField filterTextField;

    private EnhancedTextField excludeTextField;

    protected final TreeType treeType;

    protected JPanel contentPanel;

    private final String STACKTRACE_FILTER = "Stacktrace filter (and, or, not operators allowed)";
    private final String NODE_FILTER = "Node filter (and, or, not operators allowed)";

    protected AnalyzerPane(AnalyzerGroupPane parent, TreeType treeType) {
        super(new BorderLayout());

        this.parent = parent;
        this.main = parent.getMain();
        this.treeType = treeType;

        JPanel filterPanel = new JPanel(new GridLayout(0, 1));

        filterTextField = new EnhancedTextField(STACKTRACE_FILTER);
        filterTextField.setToolTipText(STACKTRACE_FILTER);
        filterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        filterTextField.addActionListener(this);
        filterPanel.add(filterTextField);

        excludeTextField = new EnhancedTextField(NODE_FILTER);
        excludeTextField.setToolTipText(NODE_FILTER);
        excludeTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        excludeTextField.addActionListener(this);
        filterPanel.add(excludeTextField);

        contentPanel = new JPanel();

        add(filterPanel, BorderLayout.PAGE_START);
        add(contentPanel, BorderLayout.CENTER);

        transform();

//		main.getInstrumentationPane().addListener(this);
    }

    public void appendCurrentSelectionToBranchFilter(boolean negate) {
        appendFilter(negate, filterTextField);
        refresh();
    }

    public void appendCurrentSelectionToNodeFilter(boolean negate) {
        appendFilter(negate, excludeTextField);
        refresh();
    }

    private void appendFilter(boolean negate, EnhancedTextField textField) {
        StringBuilder filter = new StringBuilder();
        String currentFilter = textField.getText();
        if (currentFilter != null && currentFilter.trim().length() > 0) {
            filter.append(currentFilter).append(" and ");
        }
        if (negate) {
            filter.append("not ");
        }
        filter.append(getPresentationHelper().getFullname(getSelectedNode()));
        textField.setText(filter.toString().trim());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refresh();
    }

    public String getStacktraceFilter() {
        return filterTextField.getText();
    }

    public void setStacktraceFilter(String filter) {
        filterTextField.setText(filter);
    }

    public String getNodeFilter() {
        return excludeTextField.getText();
    }

    public void setNodeFilter(String filter) {
        excludeTextField.setText(filter);
    }

    private Filter<RealNodePath> parseBranchFilter() {
        Filter<RealNodePath> complexFilter = null;
        String filter = getStacktraceFilter();
        if (filter != null) {
            BranchFilterFactory atomicFactory = new BranchFilterFactory(getPresentationHelper());
            try {
                complexFilter = OQLFilterBuilder.getFilter(filter, atomicFactory);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error parsing stacktrace filter '" + filter + "'. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return complexFilter;
    }

    private Filter<NodeID> parseNodeFilter() {
        String excludeFilter = getNodeFilter();
        if (excludeFilter != null) {
            NodeFilterFactory atomicFactory = new NodeFilterFactory(getPresentationHelper());
            try {
                nodeFilter = OQLFilterBuilder.getFilter(excludeFilter, atomicFactory);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error parsing node filter '" + excludeFilter + "'. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                nodeFilter = null;
            }
        } else {
            nodeFilter = null;
        }
        return nodeFilter;
    }

    void transform() {
        Filter<RealNodePath> branchFilter = parseBranchFilter();
        Filter<NodeID> nodeFilter = parseNodeFilter();

        AnalyzerService analyzerService = parent.getAnalyzerService();
        workNode = analyzerService.buildTree(branchFilter, nodeFilter, treeType);
    }

    public void instrumentCurrentMethod() {
        NodeID nodeID = getSelectedNode().getId();
        Object attachment = nodeID.getAttachment();
        if (attachment != null && attachment instanceof StackTraceElement) {
            StackTraceElement el = (StackTraceElement) attachment;
            main.addSubscription(new SimpleSubscription(el.getClassName(), el.getMethodName(), true));
        }
    }

    public void instrumentCurrentNode() {
        if (nodeFilter == null) {
            main.addSubscription(new RealNodePathSubscription(toStackTrace(getSelectedNode().getRealNodePath().getFullPath()), true));
        } else {
            JOptionPane.showMessageDialog(this,
                "Instrumentation impossible when packages are skipped. Please remove the exclusion criteria and try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void decompileClassOfCurrentNode() {
    	NodeID nodeID = getSelectedNode().getId();
    	Object attachment = nodeID.getAttachment();
        if (attachment != null && attachment instanceof StackTraceElement) {
            StackTraceElement el = (StackTraceElement) attachment;
            String className = el.getClassName();
            DecompilerFrame decompilerFrame = new DecompilerFrame(getMain(), className);
        }
    }

    private StackTraceElement[] toStackTrace(List<NodeID> path) {
        int size = path.size();
        StackTraceElement[] result = new StackTraceElement[size];
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                NodeID id = path.get(i);
                Object attachment = id.getAttachment();
                if (attachment != null && attachment instanceof StackTraceElement) {
                    StackTraceElement el = (StackTraceElement) attachment;
                    result[size - i - 1] = new StackTraceElement(el.getClassName(), el.getMethodName(), null, -1);
                }
            }
        }
        return result;
    }

    public abstract void refreshDisplay();

    protected abstract AnalysisNode getSelectedNode();

    public Session getMain() {
        return main;
    }

//	public void onSelection(Set<InstrumentSubscription> subscriptions) {
//		repaint();
//	}

    public NodePresentationHelper getPresentationHelper() {
        return parent.getPresentationHelper();
    }

    public void resetFocus() {
        requestFocusInWindow();
    }

    public void refresh() {
        transform();
        refreshDisplay();
    }
}
