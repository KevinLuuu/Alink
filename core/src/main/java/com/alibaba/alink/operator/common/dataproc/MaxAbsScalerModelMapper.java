package com.alibaba.alink.operator.common.dataproc;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;

import com.alibaba.alink.common.mapper.ModelMapper;
import com.alibaba.alink.common.model.RichModelDataConverter;
import com.alibaba.alink.common.utils.OutputColsHelper;
import com.alibaba.alink.common.utils.TableUtil;
import com.alibaba.alink.params.dataproc.SrtPredictMapperParams;

import java.util.List;

/**
 * This mapper changes a row values to range [-1, 1] by dividing through the maximum absolute value of each feature.
 */
public class MaxAbsScalerModelMapper extends ModelMapper {
	private static final long serialVersionUID = -358829029280240904L;
	private int[] selectedColIndices;
	private double[] maxAbs;
	private OutputColsHelper predictResultColsHelper;

	/**
	 * Constructor.
	 *
	 * @param modelSchema the model schema.
	 * @param dataSchema  the data schema.
	 * @param params      the params.
	 */
	public MaxAbsScalerModelMapper(TableSchema modelSchema, TableSchema dataSchema, Params params) {
		super(modelSchema, dataSchema, params);
		String[] selectedColNames = RichModelDataConverter.extractSelectedColNames(modelSchema);
		TypeInformation[] selectedColTypes = RichModelDataConverter.extractSelectedColTypes(modelSchema);
		this.selectedColIndices = TableUtil.findColIndicesWithAssert(dataSchema, selectedColNames);

		String[] outputColNames = params.get(SrtPredictMapperParams.OUTPUT_COLS);
		if (outputColNames == null) {
			outputColNames = selectedColNames;
		}

		this.predictResultColsHelper = new OutputColsHelper(dataSchema,
			outputColNames, selectedColTypes, null);
	}

	/**
	 * Load model from the list of Row type data.
	 *
	 * @param modelRows the list of Row type data.
	 */
	@Override
	public void loadModel(List <Row> modelRows) {
		MaxAbsScalerModelDataConverter converter = new MaxAbsScalerModelDataConverter();
		maxAbs = converter.load(modelRows);
	}

	/**
	 * Get the table schema(includes column names and types) of the calculation result.
	 *
	 * @return the table schema of output Row type data.
	 */
	@Override
	public TableSchema getOutputSchema() {
		return this.predictResultColsHelper.getResultSchema();
	}

	/**
	 * Map operation method.
	 *
	 * @param row the input Row type data.
	 * @return one Row type data.
	 * @throws Exception This method may throw exceptions. Throwing
	 * an exception will cause the operation to fail.
	 */
	@Override
	public Row map(Row row) throws Exception {
		if (null == row) {
			return null;
		}
		Row r = new Row(selectedColIndices.length);
		for (int i = 0; i < this.selectedColIndices.length; i++) {
			Object obj = row.getField(this.selectedColIndices[i]);
			if (null != obj) {
				double d;
				if (obj instanceof Number) {
					d = ((Number) obj).doubleValue();
				} else {
					d = Double.parseDouble(obj.toString());
				}
				r.setField(i, ScalerUtil.maxAbsScaler(this.maxAbs[i], d));
			}
		}
		return this.predictResultColsHelper.getResultRow(row, r);
	}
}
