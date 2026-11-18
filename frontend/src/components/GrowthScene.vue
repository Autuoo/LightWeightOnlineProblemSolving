<template><div ref="host" class="growth-scene" aria-hidden="true" /></template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import * as THREE from 'three'

const host = ref(null)
const emit = defineEmits(['stage'])
let renderer, frame, observer
let lastStage = -1
const scene = new THREE.Scene()
scene.background = new THREE.Color(0xf4fbf8)
scene.fog = new THREE.FogExp2(0xf4fbf8, .016)
const camera = new THREE.PerspectiveCamera(55, 1, .1, 130)
const cyan = 0x0d9488, blue = 0x62afa2, amber = 0x65a342
const mat = (c, o = 1) => new THREE.MeshBasicMaterial({ color:c, transparent:o < 1, opacity:o, side:THREE.DoubleSide, depthWrite:o === 1 })
function box(w,h,d,c,x,y,z,o=1) { const m=new THREE.Mesh(new THREE.BoxGeometry(w,h,d),mat(c,o));m.position.set(x,y,z);scene.add(m);return m }
function wire(w,h,d,c,x,y,z) { const m=new THREE.LineSegments(new THREE.EdgesGeometry(new THREE.BoxGeometry(w,h,d)),new THREE.LineBasicMaterial({color:c,transparent:true,opacity:.85}));m.position.set(x,y,z);scene.add(m) }
function line(a,b,c) { const g=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(...a),new THREE.Vector3(...b)]);scene.add(new THREE.Line(g,new THREE.LineBasicMaterial({color:c,transparent:true,opacity:.65}))) }
function codePlane(lines,x,y,z,w=5,h=1.45) {
  const canvas=document.createElement('canvas');canvas.width=1200;canvas.height=350
  const c=canvas.getContext('2d')
  c.clearRect(0,0,1200,350)
  c.font='bold 62px Consolas, monospace'
  lines.forEach((text,i)=>{
    c.fillStyle=i===0?'#087a69':'#176252';c.fillText(text,35,92+i*101)
  })
  const texture=new THREE.CanvasTexture(canvas);texture.colorSpace=THREE.SRGBColorSpace
  const m=new THREE.Mesh(new THREE.PlaneGeometry(w,h),new THREE.MeshBasicMaterial({map:texture,transparent:true,side:THREE.DoubleSide,depthWrite:false}));m.position.set(x,y,z);scene.add(m)
}
function build() {
  for(let z=12;z>=-59;z-=2) {
    line([-7,-3.45,z],[7,-3.45,z],0x9ccbbd)
    if(z%4===0) { for(const x of [-7,7]) {box(.055,7,.055,0x83bcae,x,0,z,.7);line([x,3.5,z],[x,3.5,z-4],0x83bcae)} }
  }
  for(let x=-7;x<=7;x+=1.75) line([x,-3.44,12],[x,-3.44,-59],0xa8d4c5)
  for(const x of [-1.75,1.75]) line([x,-3.42,12],[x,-3.42,-59],cyan)
  const stages=[2,-12,-26,-40]
  stages.forEach((z,i)=>{
    const gz=z-5
    for(let k=0;k<4;k++) wire(12-k*.2,7-k*.16,.08,i===3?amber:cyan,0,0,gz-k*.2)
  })
  codePlane(['print("Hello, World!")'],-2.15,.45,1,5.3,1.5)
  for(let i=0;i<16;i++){const a=i*2.3;box(.11,.11,.11,i%3?cyan:blue,2.2+Math.sin(a)*1.5,Math.cos(a)*1.5,1-i*.15,.75)}
  codePlane(['for (let i = 0; i < n; i++)','  answer += values[i]'],-2.6,.7,-13,5.2,1.6)
  for(let i=0;i<9;i++){const h=.4+i*.32,x=.2+i*.58;wire(.46,h,.7,i<5?blue:cyan,x,-3.2+h/2,-13-i*.27);box(.44,h,.68,i<5?blue:cyan,x,-3.2+h/2,-13-i*.27,.16)}
  codePlane(['queue.push(nextNode)','visited.add(nextNode)'],-2.6,.7,-27,5.2,1.6)
  const nodes=[[.2,1.3],[2,2.1],[3.9,1.2],[1.1,-.5],[3,-1.2],[5,-.4]]
  for(const [a,b] of [[0,1],[1,2],[0,3],[1,4],[2,5],[3,4],[4,5]])line([nodes[a][0],nodes[a][1],-27],[nodes[b][0],nodes[b][1],-27],0x65a996)
  nodes.forEach(([x,y],i)=>{box(.4,.4,.4,i<3?cyan:blue,x,y,-27);wire(.68,.68,.68,i<3?cyan:blue,x,y,-27)})
  codePlane(['dp[i] = min(dp[i], cost)','return dp[n]'],-2.6,.7,-41,5.2,1.6)
  for(let i=0;i<5;i++)for(let j=0;j<5;j++){const x=.2+i,y=-1.6+j,z=-41-(i+j)*.2;wire(.7,.7,.7,i+j>5?amber:cyan,x,y,z);if(i+j===5)box(.38,.38,.38,amber,x,y,z,.4)}
  for(let i=0;i<100;i++){const x=Math.sin(i*17.13)*6.4,y=Math.cos(i*9.71)*3.2;box(.03,.03,.03,i%5?cyan:blue,x,y,8-i*.65,.65)}
}
function ease(a,b,t){const x=Math.max(0,Math.min(1,(t-a)/(b-a)));return x*x*(3-2*x)}
onMounted(()=>{
  build()
  try{renderer=new THREE.WebGLRenderer({antialias:true,powerPreference:'low-power'})}catch{return}
  renderer.setPixelRatio(Math.min(devicePixelRatio||1,1.5));renderer.outputColorSpace=THREE.SRGBColorSpace;host.value.appendChild(renderer.domElement)
  const resize=()=>{const w=host.value.clientWidth,h=host.value.clientHeight;camera.aspect=w/h;camera.updateProjectionMatrix();renderer.setSize(w,h)}
  observer=new ResizeObserver(resize);observer.observe(host.value);resize()
  const reduce=matchMedia('(prefers-reduced-motion: reduce)'),start=performance.now()
  const animate=now=>{
    const t=reduce.matches?.04:((now-start)/19000)%1,travel=ease(.06,.80,t)
    const stage=t<.28?0:t<.46?1:t<.64?2:3
    if(stage!==lastStage){lastStage=stage;emit('stage',stage)}
    camera.position.set(Math.sin(travel*Math.PI*5)*1.5,.25+Math.sin(travel*Math.PI*3)*.4,11-travel*51)
    camera.fov=18+40*ease(.05,.17,t)-40*ease(.81,.96,t);camera.updateProjectionMatrix()
    camera.lookAt(0,.05,camera.position.z-12);renderer.render(scene,camera)
    frame=requestAnimationFrame(animate)
  }
  frame=requestAnimationFrame(animate)
})
onUnmounted(()=>{cancelAnimationFrame(frame);observer?.disconnect();scene.traverse(o=>{o.geometry?.dispose();o.material?.map?.dispose();o.material?.dispose?.()});renderer?.dispose()})
</script>

<style scoped>
.growth-scene{position:absolute;inset:0;overflow:hidden;background:#f4fbf8}
.growth-scene :deep(canvas){display:block;width:100%;height:100%}
.growth-scene:before,.growth-scene:after{content:'';position:absolute;inset:0;pointer-events:none}
.growth-scene:before{background:radial-gradient(ellipse 38% 50% at 52% 51%,#0d94881c,transparent 80%)}
.growth-scene:after{background:#f4fbf8;animation:scene-wipe 19s linear infinite;opacity:0}
@keyframes scene-wipe{0%{opacity:1}4%{opacity:0}91%{opacity:0}97%,100%{opacity:1}}
@media(prefers-reduced-motion:reduce){.growth-scene:after{animation:none;opacity:0}}
</style>
