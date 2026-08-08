<script setup>
import { onMounted } from 'vue'
import * as echarts from 'echarts'
import { queryEmpJobDataApi, queryEmpGenderDataApi } from '@/api/report'

//钩子函数 - 加载报表
onMounted(() => {
  loadJobChart() //加载职位统计报表
  loadGenderChart() //加载性别统计报表
})

//获取职位统计报表
const loadJobChart = async () => {
  let result = await queryEmpJobDataApi();
  let jobList = result.data.jobList;
  let dataList = result.data.dataList;

  initJobChart(jobList, dataList)
}

//获取性别统计报表
const loadGenderChart = async () => {
  let result = await queryEmpGenderDataApi();
  // 将 {pos, num} 转换为 ECharts 饼图需要的 {name, value} 格式
  let genderDataList = result.data.map(item => ({
    name: item.pos,
    value: item.num
  }));
  initGenderChart(genderDataList)
}


function initJobChart(jobList, dataList) {
  var dom = document.getElementById('container1'); echarts.dispose(dom);
  var myChart = echarts.init(dom);
  // 绘制图表
  myChart.setOption({
    title: {
      text: '员工职位统计',
      textStyle: { fontSize: 20 },
      left: 'center'
    },
    grid: { left: '5%', right: '10%', bottom: '3%', containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', name: '人数' },
    yAxis: {
      type: 'category',
      data: jobList,
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

function initGenderChart(genderDataList) {
  var dom = document.getElementById('container2'); echarts.dispose(dom);
  var myChart = echarts.init(dom);
  let option = {
    title: {
      text: '员工性别统计',
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
        name: '性别',
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
        data: genderDataList
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