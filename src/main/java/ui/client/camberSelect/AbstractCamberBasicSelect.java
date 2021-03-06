package ui.client.camberSelect;

import com.google.gwt.user.client.Timer;
import common.client.Func;
import common.client.Jso;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import react.client.*;
import ui.client.select2.*;
import ui.client.util.Lodash;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractCamberBasicSelect<VALUE, P extends AbstractCamberBasicSelect.CamberSelectProps<VALUE>> extends Component<P, AbstractCamberBasicSelect.State<VALUE>> {
//    private final Loggly log = Loggly.get(AbstractCamberBasicSelect.class);

    @Inject
    Select2 select2;

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Render
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected ReactElement render(ReactComponent<P, State<VALUE>> $this) {
        return select2.$($ -> {
            Select2Options opts = Jso.create();
            opts.setMultiple($this.props.isMultiple());
            opts.setAllowClear($this.props.isAllowClear() && !$this.props.isMultiple());
            opts.setPlaceholder($this.props.getPlaceholder());
            opts.setEscapeMarkup(v -> v); // allow html content
            opts.setMinimumResultsForSearch($this.props.getMinimumResultsForSearch());
            opts.setDataAdapterFn((params, completion) -> {
                if (params.getTerm() == null || params.getTerm().isEmpty() || $this.props.isDebounceDisabled()) {
                    internalFetch($this, params.getTerm(), completion);
                    return;
                }

                if ($this.state.getDebounceTimer() != null) {
                    $this.state.getDebounceTimer().cancel();
                }

                Timer t = new Timer() {
                    @Override
                    public void run() {
                        internalFetch($this, params.getTerm(), completion);
                    }
                };
                t.schedule(250);
                $this.setState(s -> s.setDebounceTimer(t));
            });
            $.setOptions(opts);

            // Handle in/out of VALUE to Select2Data[] in the select2 control

            if ($this.props.isMultiple() && $this.props.getOnMultipleValueChanged() != null) {
                $.setOnValueChange(select2Datas -> {
                    if (select2Datas == null || select2Datas.length < 1) {
                        $this.setState(s -> {
                            s.setSelectedDataMap(new HashMap<>());
                            s.setSelect2Value(null);
                        });
                        $this.props.getOnMultipleValueChanged().run(null);
                    } else {
                        HashMap<String, VALUE> selectedDataMap = new HashMap<>();
                        List<VALUE> values = new ArrayList<>();
                        for (Select2Data d : select2Datas) {
                            VALUE value = $this.state.getSelectedDataMap().get(d.getId());
                            if (value == null) {
                                value = $this.state.getDataMap().get(d.getId());
                            }
                            if (value != null) {
                                values.add(value);
                                selectedDataMap.put(d.getId(), value);
                            }
                        }
                        $this.setState(s -> {
                            s.setSelectedDataMap(selectedDataMap);
                            s.setSelect2Value(select2Datas);
                        });
                        $this.props.getOnMultipleValueChanged().run(values);
                    }
                });
            } else if (!$this.props.isMultiple() && $this.props.getOnValueChanged() != null) {

                $.setOnValueChange(select2Datas -> {
                    if (select2Datas == null || select2Datas.length < 1) {
                        $this.setState(s -> {
                            s.setSelectedDataMap(new HashMap<>());
                            s.setSelect2Value(null);
                        });
                        $this.props.getOnValueChanged().run(null);
                    } else {

                        // check maps to match VALUE object
                        VALUE value = $this.state.getSelectedDataMap().get(select2Datas[0].getId());
                        if (value == null) {
                            value = $this.state.getDataMap().get(select2Datas[0].getId());
                        }

                        if (value != null) {
                            HashMap<String, VALUE> selectedDataMap = new HashMap<>();
                            selectedDataMap.put(select2Datas[0].getId(), value);
                            $this.setState(s -> {
                                s.setSelectedDataMap(selectedDataMap);
                                s.setSelect2Value(select2Datas);
                            });
                            $this.props.getOnValueChanged().run(value);
                        }
                    }
                });
            }

            // set select2 value
            $.setValue($this.state.getSelect2Value());


            // pass through props to select2
            $.setOpen($this.props.isOpen());
            $.setClose($this.props.isClose());
            $.setEvents($this.props.getSelect2Events());
            $.setDisabled($this.props.isDisabled());
            $.setStyle($this.props.getStyle());
            $.className($this.props.getClassName());
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Component Lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public P getDefaultProps() {
        P props = Jso.create();
        props.setAllowClear(true);
        props.setPlaceholder("");
        return props;
    }

    @Override
    public State<VALUE> getInitialState() {
        State<VALUE> state = Jso.create();
        state.setDataMap(new HashMap<>());
        state.setSelectedDataMap(new HashMap<>());
        return state;
    }

    @Override
    protected void intakeProps(ReactComponent<P, State<VALUE>> $this, P nextProps) {
        super.intakeProps($this, nextProps);
        boolean curPropsIsNull = $this.props == null;

        if (nextProps.isMultiple()) {
            boolean notEqualPropsValues = $this.props == null || !Lodash.isEqual($this.props.getMultipleValue(), nextProps.getMultipleValue());
            if (curPropsIsNull || notEqualPropsValues) {
                if (nextProps.getMultipleValue() != null) {
                    List<Select2Data> datas = new ArrayList<>();
                    for (VALUE v : nextProps.getMultipleValue()) {
                        datas.add(translate(v));
                    }
                    $this.setState(s -> s.setSelect2Value(((Select2Data[]) datas.toArray())));
                } else {
                    $this.setState(s -> s.setSelect2Value(null));
                }
            }
        } else {
            boolean notEqualPropsValues = $this.props == null || !Lodash.isEqual($this.props.getValue(), nextProps.getValue());
            if (curPropsIsNull || notEqualPropsValues) {
                if (nextProps.getValue() != null) {
                    $this.setState(s -> s.setSelect2Value(new Select2Data[]{translate(nextProps.getValue())}));
                } else {
                    $this.setState(s -> s.setSelect2Value(null));
                }
            }
        }
    }

    @Override
    protected boolean shouldComponentUpdate(ReactComponent<P, State<VALUE>> $this, P nextProps, State<VALUE> nextState) {
        boolean notEqualProps = !Lodash.isEqual($this.props, nextProps);
        boolean notEqualSelect2Value = true; // only compare select2 value since the datamaps should not affect rendering
        if ($this.state != null && $this.state.getSelect2Value() != null && nextState != null && nextState.getSelect2Value() != null) {
            notEqualSelect2Value = !Lodash.isEqual($this.state.getSelect2Value(), nextState.getSelect2Value());
        }
        return notEqualProps || notEqualSelect2Value;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Abstract Methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract void internalFetch(ReactComponent<P, State<VALUE>> $this, String search, Func.Run1<Select2DataAdapterResponse> completion);

    protected abstract Select2Data translate(VALUE value);

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Work
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public interface FetchCompletionHandler<V> {
        void onComplete(List<V> data);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Args / Props / State / Route
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @JsType(isNative = true)
    public interface CamberSelectProps<V> extends BaseProps {

        @JsProperty
        V getValue();

        @JsProperty
        void setValue(V value);

        @JsProperty
        List<V> getMultipleValue();

        @JsProperty
        void setMultipleValue(List<V> value);

        @JsProperty
        boolean isMultiple();

        @JsProperty
        void setMultiple(boolean multiple);

        @JsProperty
        String getPlaceholder();

        @JsProperty
        void setPlaceholder(String placeholder);

        @JsProperty
        Func.Run1<V> getOnValueChanged();

        @JsProperty
        void setOnValueChanged(Func.Run1<V> onValueChanged);

        @JsProperty
        Func.Run1<List<V>> getOnMultipleValueChanged();

        @JsProperty
        void setOnMultipleValueChanged(Func.Run1<List<V>> onValueChanged);

        @JsProperty
        boolean isDisabled();

        @JsProperty
        void setDisabled(boolean disabled);

        @JsProperty
        String getClassName();

        @JsProperty
        void setClassName(String className);

        @JsProperty
        StyleProps getStyle();

        @JsProperty
        void setStyle(StyleProps style);

        @JsProperty
        boolean isAllowClear();

        @JsProperty
        void setAllowClear(boolean allowClear);

        @JsProperty
        double getMinimumResultsForSearch();

        @JsProperty
        void setMinimumResultsForSearch(double minimumResultsForSearch);

        @JsProperty
        boolean isOpen();

        @JsProperty
        void setOpen(boolean open);

        @JsProperty
        boolean isClose();

        @JsProperty
        void setClose(boolean close);

        @JsProperty
        Select2Events getSelect2Events();

        @JsProperty
        void setSelect2Events(Select2Events select2Events);

        @JsProperty
        boolean isDebounceDisabled();

        @JsProperty
        void setDebounceDisabled(boolean debounceDisabled);
    }

    @JsType(isNative = true)
    public interface State<V> {
        @JsProperty
        Select2Data[] getSelect2Value();

        @JsProperty
        void setSelect2Value(Select2Data[] select2Value);

        @JsProperty
        HashMap<String, V> getSelectedDataMap();

        @JsProperty
        void setSelectedDataMap(HashMap<String, V> dataMap);

        @JsProperty
        HashMap<String, V> getDataMap();

        @JsProperty
        void setDataMap(HashMap<String, V> dataMap);

        @JsProperty
        Timer getDebounceTimer();

        @JsProperty
        void setDebounceTimer(Timer debounceTimer);
    }
}
