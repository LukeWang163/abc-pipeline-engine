package base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension;

import base.operators.operator.timeseries.timeseriesanalysis.datamodel.exception.IllegalSeriesLengthException;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.IndexDimension;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.dimension.interfaces.SeriesValues;
import base.operators.operator.timeseries.timeseriesanalysis.datamodel.newmodel.series.SeriesBuilder;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IllegalIndexArgumentException;
import base.operators.operator.timeseries.timeseriesanalysis.exception.IndexArgumentsDontMatchException;
import base.operators.operator.timeseries.timeseriesanalysis.tools.SeriesUtils;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractValuesListDimension<T> extends AbstractDimension<T> {
   protected List<T> valuesList;

   protected AbstractValuesListDimension(List<T> valuesList, boolean isIndexDimension, SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType) {
      super(isIndexDimension, indexType, valuesType);
      SeriesUtils.checkList(valuesList, this.getDimensionTypeName() + " list");
      if (isIndexDimension) {
         this.isValidIndexDimension(valuesList);
      }

      this.valuesList = new ArrayList(valuesList);
   }

   protected AbstractValuesListDimension(List<T> valuesList, boolean isIndexDimension, SeriesBuilder.IndexType indexType, SeriesBuilder.ValuesType valuesType, String name) {
      super(isIndexDimension, indexType, valuesType, name);
      SeriesUtils.checkList(valuesList, this.getDimensionTypeName() + " list");
      if (isIndexDimension) {
         this.isValidIndexDimension(valuesList);
      }

      this.valuesList = new ArrayList(valuesList);
   }

   protected T getValueImpl(int index) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         return this.valuesList.get(index);
      }
   }

   protected List getValuesImpl() {
      return new ArrayList(this.valuesList);
   }

   protected void setValueImpl(int index, T value) {
      if (index < 0) {
         throw new IllegalIndexArgumentException("index", IllegalIndexArgumentException.IllegalIndexArgumentType.NEGATIVE);
      } else if (index >= this.getLength()) {
         throw new IndexArgumentsDontMatchException("index", "series", index, this.getLength(), IndexArgumentsDontMatchException.MisMatchType.LARGER_EQUAL);
      } else {
         this.valuesList.set(index, value);
      }
   }

   protected void setValuesImpl(List values) {
      SeriesUtils.checkList(values, this.getDimensionTypeName() + " list");
      if (values.size() != this.getLength()) {
         throw new IllegalSeriesLengthException(this.getDimensionTypeName(), "series", values.size(), this.getLength());
      } else {
         this.valuesList = new ArrayList(values);
      }
   }

   public int getLength() {
      return this.valuesList.size();
   }

   public boolean equalIndexDimension(IndexDimension indexDimension) {
      return this.equalAbstractValuesListDimension(indexDimension);
   }

   public boolean equalSeriesValues(SeriesValues seriesValues) {
      return this.equalAbstractValuesListDimension(seriesValues);
   }

   private boolean equalAbstractValuesListDimension(Object object) {
      if (!(object instanceof AbstractValuesListDimension)) {
         return false;
      } else {
         AbstractValuesListDimension abstractValuesListDimension = (AbstractValuesListDimension)object;
         return abstractValuesListDimension.getLength() == this.getLength() && abstractValuesListDimension.getName().equals(this.getName()) && abstractValuesListDimension.getValuesImpl().equals(this.valuesList);
      }
   }

   public boolean hasFixLength() {
      return true;
   }
}
