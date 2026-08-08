<script setup>
import { onMounted } from 'vue'
import * as echarts from 'echarts'
import { queryStudentCountDataApi, queryEmpGenderDataApi, queryStudentDegreeDataApi } from '@/api/report'

//钩子函数 - 加载报表
onMounted(() => {
  loadStudentCountChart() //加载班级人数报表
  loadDegreeChart() //加载性别统计报表
})

//获取职位统计报表
const loadStudentCountChart = async () => {
  let result = await queryStudentCountDataApi();
  let clazzList = result.data.clazzList;
  let dataList = result.data.dataList;

  initStudentCountChart(clazzList, dataList)
}

//获取学历统计报表
const loadDegreeChart = async () => {
  let result = await queryStudentDegreeDataApi();
  initDegreeChart(result.data)
}

//班级人数统计报表
function initStudentCountChart(clazzList, dataList) {
  var dom = document.getElementById('container1');
  echarts.dispose(dom); // 先销毁旧实例
  var myChart = echarts.init(dom);
  // 绘制图表
  myChart.setOption({
    title: {
      text: '班级人数统计',
      textStyle: { fontSize: 20 },
      left: 'center'
    },
    grid: { left: '5%', right: '10%', bottom: '3%', containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', name: '人数' },
    yAxis: {
      type: 'category',
      data: clazzList,
      inverse: true,
      axisLabel: { fontSize: 13 }
    },
    series: [{
      name: '人数',
      type: 'bar',
      data: dataList,
      label: { show: true, position: 'right', fontSize: 13 },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#007fa4' },
          { offset: 1, color: '#00d072' }
        ])
      }
    }]
  });
}

function initDegreeChart(degreeDataList) {
  var dom = document.getElementById('container2');
  echarts.dispose(dom);
  var myChart = echarts.init(dom);
  let option = {
    title: {
      text: '学员学历统计',
      subText: '',
      textStyle: {
        fontSize: 20
      },
      left: 'center'
    },
    grid:{
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel:true
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      top: '10%',
      left: 'center'
    },
    series: [
      {
        name: '学历',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        top: '5%',
        itemStyle: {
          borderRadius: 5,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        data: degreeDataList
      }
    ]
  };
  // 绘制图表
  myChart.setOption(option);
}

</script>

<template>
  <div class="report-wrapper">
    <div class="report_container" id="container1">
    </div>
    <div class="report_container" id="container2">
    </div>
  </div>
</template>

<style scoped>
.report-wrapper {
  height: calc(100vh - 64px);
  display: flex;
}

.report_container {
  flex: 1;
  height: 100%;
}

#container1 {
  border-right: 1px dashed #ccc;
}
</style>