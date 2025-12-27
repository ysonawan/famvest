import {
  Component,
  Input,
  OnInit,
  AfterViewInit,
  ViewChild,
  ElementRef,
  OnDestroy,
  OnChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxEchartsModule } from 'ngx-echarts';
import { EChartsCoreOption } from 'echarts/core';

interface SeriesInput {
  name: string;
  data: (number | [number, number])[];
  type?: string;
  smooth?: boolean;
}

@Component({
  selector: 'app-timeline-chart',
  standalone: true,
  imports: [CommonModule, NgxEchartsModule],
  templateUrl: './timeline-chart.component.html',
  styleUrls: ['./timeline-chart.component.css']
})
export class TimelineChartComponent implements OnInit, AfterViewInit, OnDestroy, OnChanges {
  @ViewChild('chartContainer') chartContainer!: ElementRef;

  @Input() title: string = '';
  @Input() xAxisLabels: string[] = [];
  @Input() series: SeriesInput[] = [];
  @Input() legendLabels: string[] = [];
  @Input() chartHeight: string = '280';
  @Input() showDataZoom: boolean = true;

  initOpts = { renderer: 'canvas', width: 'auto', height: 'auto' };
  chartOptions: EChartsCoreOption = {};

  private resizeListener = () => this.updateChartSize();
  private isMobile: boolean = false;

  ngOnInit(): void {
    this.checkMobile();
    this.buildChartOptions();
  }

  ngOnChanges(): void {
    this.buildChartOptions();
  }

  ngAfterViewInit(): void {
    this.updateChartSize();
    window.addEventListener('resize', this.resizeListener);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.resizeListener);
  }

  private checkMobile(): void {
    this.isMobile = window.innerWidth < 768;
  }

  private updateChartSize(): void {
    this.checkMobile();
    if (this.chartContainer) {
      const width = this.chartContainer.nativeElement.parentElement.offsetWidth;
      const height = this.isMobile ?
        Math.min(300, window.innerHeight * 0.4) :
        parseInt(this.chartHeight);

      this.initOpts = {
        renderer: 'canvas',
        width: width + 'px',
        height: height + 'px'
      };
      this.buildChartOptions();
    }
  }

  private buildChartOptions(): void {
    this.chartOptions = {
      title: {
        text: this.title,
        textStyle: {
          fontSize: this.isMobile ? 12 : 14
        },
        left: this.isMobile ? 'center' : 'left'
      },
      tooltip: {
        trigger: 'axis',
        confine: true,
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#ccc',
        borderWidth: 1,
        textStyle: {
          fontSize: this.isMobile ? 11 : 12
        },
        formatter: (params: any) => {
          const header = params[0].axisValue;
          const body = params
            .map((item: any) => {
              const value = item.value;
              const formatted = this.formatValue(item.seriesName, value);
              return `${item.marker} ${item.seriesName}: ${formatted}`;
            })
            .join('<br/>');
          return `<strong>${header}</strong><br/>${body}`;
        }
      },
      legend: {
        data: this.legendLabels,
        orient: 'horizontal',
        top: this.isMobile ? 25 : 5,
        left: 'center',
        textStyle: {
          fontSize: this.isMobile ? 10 : 12
        },
        itemWidth: this.isMobile ? 15 : 25,
        itemHeight: this.isMobile ? 8 : 14
      },
      grid: {
        left: this.isMobile ? '5%' : '3%',
        right: this.isMobile ? '5%' : '4%',
        bottom: this.isMobile ? '12%' : '10%',
        top: this.isMobile ? '25%' : '20%',
        containLabel: true
      },
      toolbox: {
        show: !this.isMobile,
        top: 0,
        right: 50,
        feature: {
          dataZoom: { yAxisIndex: 'none' },
          restore: {},
          saveAsImage: {}
        }
      },
      dataZoom: this.showDataZoom
        ? [
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: this.isMobile ? 20 : 30,
            textStyle: {
              fontSize: this.isMobile ? 10 : 12
            }
          },
          { type: 'inside', start: 0, end: 100 }
        ]
        : [],
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: this.xAxisLabels,
        axisLabel: {
          fontSize: this.isMobile ? 9 : 11,
          rotate: this.isMobile ? 45 : 0,
          interval: 'auto',
          hideOverlap: true,
          showMaxLabel: true,
          showMinLabel: true
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          fontSize: this.isMobile ? 9 : 11,
          formatter: (value: number) => {
            if (this.isMobile && Math.abs(value) >= 1000) {
              return (value / 1000).toFixed(0) + 'k';
            }
            return value.toLocaleString();
          }
        }
      },
      series: this.series.map(s => ({
        type: 'line',
        smooth: s.smooth ?? true,
        lineStyle: {
          width: this.isMobile ? 1.5 : 2
        },
        symbol: this.isMobile ? 'none' : 'circle',
        symbolSize: this.isMobile ? 3 : 4,
        ...s
      }))
    };
  }

  private formatValue(seriesName: string, value: number): string {
    if (value == null || isNaN(value)) {
      return '0.00';
    }

    const options = {
      minimumFractionDigits: this.isMobile ? 0 : 2,
      maximumFractionDigits: 2
    };

    if (seriesName.includes('%')) {
      return `${value.toFixed(this.isMobile ? 1 : 2)}%`;
    }

    if (seriesName.toLowerCase().includes('pnl')) {
      return `${value >= 0 ? '+' : ''}₹${value.toLocaleString(undefined, options)}`;
    }

    return `₹${value.toLocaleString(undefined, options)}`;
  }
}
