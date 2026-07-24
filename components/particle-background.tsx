"use client"

import { useEffect, useRef } from "react"
import * as THREE from "three"

const DESKTOP_PARTICLES = 18000
const TABLET_PARTICLES = 10000
const MOBILE_PARTICLES = 6000

const vertexShader = `
  uniform float uTime;
  uniform float uPointSize;
  attribute vec3 aParticleColor;
  varying vec3 vParticleColor;
  void main() {
    vParticleColor = aParticleColor;
    vec3 pos = position;
    float breath = sin(uTime * 1.3 + position.x * .32) * cos(uTime * 1.1 + position.y * .26);
    pos += normalize(pos) * breath * .18;
    pos.x += sin(uTime * .28 + position.z) * .08;
    vec4 mvPosition = modelViewMatrix * vec4(pos, 1.0);
    gl_PointSize = (uPointSize / -mvPosition.z) * (1.0 + sin(uTime * 2.4 + length(pos) * .18) * .25);
    gl_Position = projectionMatrix * mvPosition;
  }
`

const fragmentShader = `
  varying vec3 vParticleColor;
  void main() {
    float distanceToCenter = distance(gl_PointCoord, vec2(.5));
    if (distanceToCenter > .5) discard;
    float alpha = pow(1.0 - distanceToCenter * 2.0, 1.7);
    gl_FragColor = vec4(vParticleColor, alpha * .72);
  }
`

export function ParticleBackground({ variant = "public" }: { variant?: "public" | "auth" | "static" }) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    const width = window.innerWidth
    const particleCount = width < 640 ? MOBILE_PARTICLES : width < 1024 ? TABLET_PARTICLES : DESKTOP_PARTICLES
    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(38, window.innerWidth / window.innerHeight, 0.1, 300)
    camera.position.z = variant === "auth" ? 52 : 46
    const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: false, powerPreference: "high-performance" })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
    renderer.setSize(window.innerWidth, window.innerHeight)
    renderer.setClearColor(0x000000, 0)
    container.appendChild(renderer.domElement)

    const positions = new Float32Array(particleCount * 3)
    const colors = new Float32Array(particleCount * 3)
    const signal = new THREE.Color(0x9ef01a)
    const white = new THREE.Color(0xf2f5f2)
    for (let index = 0; index < particleCount; index += 1) {
      const offset = index * 3
      const vertical = (Math.random() - 0.5) * 5
      const angle = Math.random() * Math.PI * 2
      const radius = (0.4 + Math.pow(Math.abs(vertical), 2.35)) * (0.72 + Math.random() * 0.58)
      positions[offset] = radius * Math.cos(angle) * 2.8
      positions[offset + 1] = vertical * 7.2
      positions[offset + 2] = radius * Math.sin(angle) * 2.8
      const color = Math.random() > 0.82 ? signal : white
      colors[offset] = color.r; colors[offset + 1] = color.g; colors[offset + 2] = color.b
    }

    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3))
    geometry.setAttribute("aParticleColor", new THREE.BufferAttribute(colors, 3))
    const material = new THREE.ShaderMaterial({
      vertexShader,
      fragmentShader,
      transparent: true,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      uniforms: { uTime: { value: 0 }, uPointSize: { value: width < 640 ? 64 : 86 } },
    })
    const points = new THREE.Points(geometry, material)
    points.scale.setScalar(variant === "auth" ? 0.82 : 1)
    scene.add(points)

    const pointer = new THREE.Vector2()
    const target = new THREE.Vector2()
    let animationFrame = 0
    let active = !document.hidden
    const clock = new THREE.Clock()

    const render = () => {
      if (!active) return
      target.lerp(pointer, 0.035)
      points.rotation.y += reducedMotion || variant === "static" ? 0 : 0.0014
      points.rotation.z = target.x * 0.08
      points.rotation.x = target.y * 0.05 + Math.sin(clock.elapsedTime * 0.12) * 0.05
      material.uniforms.uTime.value = reducedMotion ? 0 : clock.elapsedTime
      renderer.render(scene, camera)
      if (!reducedMotion && variant !== "static") animationFrame = requestAnimationFrame(render)
    }
    render()

    const onPointerMove = (event: PointerEvent) => { pointer.set(event.clientX / window.innerWidth - 0.5, event.clientY / window.innerHeight - 0.5) }
    const onResize = () => { camera.aspect = window.innerWidth / window.innerHeight; camera.updateProjectionMatrix(); renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5)); renderer.setSize(window.innerWidth, window.innerHeight) }
    const onVisibilityChange = () => { active = !document.hidden; if (active && !animationFrame && !reducedMotion && variant !== "static") render(); if (!active) { cancelAnimationFrame(animationFrame); animationFrame = 0 } }
    window.addEventListener("pointermove", onPointerMove, { passive: true })
    window.addEventListener("resize", onResize)
    document.addEventListener("visibilitychange", onVisibilityChange)

    return () => {
      cancelAnimationFrame(animationFrame)
      window.removeEventListener("pointermove", onPointerMove)
      window.removeEventListener("resize", onResize)
      document.removeEventListener("visibilitychange", onVisibilityChange)
      geometry.dispose()
      material.dispose()
      renderer.dispose()
      renderer.domElement.remove()
    }
  }, [variant])

  return <div ref={containerRef} className={`particle-background pointer-events-none fixed inset-0 ${variant === "auth" ? "opacity-30" : "opacity-20"}`} aria-hidden="true" />
}
