package com.tide.journal.ui;
import android.content.Context;import android.opengl.*;import android.view.MotionEvent;
import com.tide.journal.domain.Lessons;import java.nio.*;import javax.microedition.khronos.egl.EGLConfig;import javax.microedition.khronos.opengles.GL10;

/** Native GLES scene. Signed heights use a fixed scale; zero is never a fake sliver. */
public final class SpatialLesson extends GLSurfaceView implements GLSurfaceView.Renderer {
 public interface Pick{void picked(int index);}
 private final Pick pick;private final float[] goals=new float[3],heights=new float[3],speeds=new float[3];
 private final float[] projection=new float[16],view=new float[16],vp=new float[16],model=new float[16],mvp=new float[16];
 private final float[][] colors={{.40f,.59f,.80f,1},{.36f,.72f,.61f,1},{.87f,.68f,.47f,1}};
 private final float[] projectedX=new float[3],projectedY=new float[3];private int program,pos,normal,matrix,color,selected=-1,width,height;
 private FloatBuffer vertices,normals;private float yaw=-18,pitch=0,spread=1,lastX,lastY,startX,startY;private boolean expanded,reduced;private volatile boolean active=true;private long last;
 public SpatialLesson(Context c,Pick p){super(c);pick=p;setEGLContextClientVersion(2);setPreserveEGLContextOnPause(true);makeMesh();setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);setContentDescription("三维教学模型；左右旋转，点击柱体看解释。下方提供同等数值按钮。");}
 public void values(Lessons.Result r){float[]next=new float[3];for(int i=0;i<3;i++)next[i]=(float)(r.values[i]/r.scale*2.4);queueEvent(()->{System.arraycopy(next,0,goals,0,3);last=0;});requestRender();}
 public void expanded(boolean v){queueEvent(()->{expanded=v;last=0;});requestRender();}public void reduce(boolean v){queueEvent(()->reduced=v);requestRender();}
 public void resumeScene(){active=true;onResume();requestRender();}public void pauseScene(){active=false;onPause();}
 private void makeMesh(){float[]corners={-1,-1,-1,1,-1,-1,1,1,-1,-1,1,-1,-1,-1,1,1,-1,1,1,1,1,-1,1,1};int[][]faces={{4,5,6,7},{1,0,3,2},{0,4,7,3},{5,1,2,6},{3,7,6,2},{0,1,5,4}};float[][]ns={{0,0,1},{0,0,-1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};float[]v=new float[108],n=new float[108];int k=0;for(int f=0;f<6;f++)for(int q:new int[]{0,1,2,0,2,3})for(int d=0;d<3;d++){v[k]=corners[faces[f][q]*3+d];n[k++]=ns[f][d];}vertices=buffer(v);normals=buffer(n);}
 private static FloatBuffer buffer(float[]v){FloatBuffer b=ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(v).position(0);return b;}
 private static int shader(int type,String code){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,code);GLES20.glCompileShader(s);int[]ok={0};GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(s));return s;}
 @Override public void onSurfaceCreated(GL10 gl,EGLConfig c){int a=shader(GLES20.GL_VERTEX_SHADER,"attribute vec3 p;attribute vec3 n;uniform mat4 m;varying float light;void main(){gl_Position=m*vec4(p,1.0);light=.66+.34*max(dot(n,normalize(vec3(-.4,.8,1.0))),0.0);}");int b=shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float;uniform vec4 c;varying float light;void main(){gl_FragColor=vec4(c.rgb*light,c.a);}");program=GLES20.glCreateProgram();GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program);int[]ok={0};GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));GLES20.glDeleteShader(a);GLES20.glDeleteShader(b);pos=GLES20.glGetAttribLocation(program,"p");normal=GLES20.glGetAttribLocation(program,"n");matrix=GLES20.glGetUniformLocation(program,"m");color=GLES20.glGetUniformLocation(program,"c");GLES20.glEnable(GLES20.GL_DEPTH_TEST);last=0;}
 @Override public void onSurfaceChanged(GL10 gl,int w,int h){width=w;height=h;GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,44,(float)w/h,.1f,40);}
 @Override public void onDrawFrame(GL10 gl){GLES20.glClearColor(.916f,.941f,.918f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);long now=System.nanoTime();double dt=last==0?1.0/60:Math.min(.033,(now-last)/1e9);last=now;boolean moving=false;
  for(int i=0;i<3;i++){if(reduced){heights[i]=goals[i];speeds[i]=0;}else{speeds[i]=(float)Lessons.spring(heights[i],goals[i],speeds[i],dt);heights[i]+=speeds[i]*dt;if(Math.abs(heights[i]-goals[i])<.001&&Math.abs(speeds[i])<.005){heights[i]=goals[i];speeds[i]=0;}else moving=true;}}
  float target=expanded?1.40f:1f;if(reduced)spread=target;else if(Math.abs(target-spread)>.001){spread+=(target-spread)*Math.min(1,dt*10);moving=true;}
  float angle=(float)Math.toRadians(yaw),camera=9f;Matrix.setLookAtM(view,0,(float)Math.sin(angle)*camera,3f+pitch,(float)Math.cos(angle)*camera,0,0,0,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);
  GLES20.glEnableVertexAttribArray(pos);GLES20.glEnableVertexAttribArray(normal);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertices);GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,0,normals);
  // An open grid marks zero. No opaque floor can conceal negative values.
  float[]grid={.70f,.78f,.73f,1};for(int i=-3;i<=3;i++){cube(i,0,0,.006f,.004f,1.1f,grid);cube(0,0,i*.35f,3,.004f,.006f,grid);}
  for(int i=0;i<3;i++){float x=(i-1)*1.35f*spread,v=heights[i];if(Math.abs(v)>.00001)cube(x,v/2,0,selected==i?.36f:.30f,Math.abs(v)/2,.30f,colors[i]);
   cube(x,0,0,.40f,.008f,.008f,colors[i]);float[]a={x,v/2,0,1},clip=new float[4];Matrix.multiplyMV(clip,0,vp,0,a,0);projectedX[i]=(clip[0]/clip[3]+1)*width/2;projectedY[i]=(1-clip[1]/clip[3])*height/2;
  }
  if(moving&&active)postOnAnimation(()->{if(active)requestRender();});
 }
 private void cube(float x,float y,float z,float sx,float sy,float sz,float[]c){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(mvp,0,vp,0,model,0);GLES20.glUniformMatrix4fv(matrix,1,false,mvp,0);GLES20.glUniform4fv(color,1,c,0);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);}
 @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:startX=lastX=x;startY=lastY=y;return true;case MotionEvent.ACTION_MOVE:if(Math.abs(x-startX)>Math.abs(y-startY)&&Math.abs(x-startX)>Ui.dp(getContext(),8)){getParent().requestDisallowInterceptTouchEvent(true);float dx=x-lastX;queueEvent(()->yaw=Math.max(-55,Math.min(55,yaw+dx*.25f)));requestRender();}lastX=x;lastY=y;return true;case MotionEvent.ACTION_UP:if(Math.hypot(x-startX,y-startY)<Ui.dp(getContext(),10)){queueEvent(()->{double best=Double.MAX_VALUE;int hit=0;for(int i=0;i<3;i++){double d=Math.hypot(projectedX[i]-x,projectedY[i]-y);if(d<best){best=d;hit=i;}}if(best<Ui.dp(getContext(),80)){selected=hit;final int h=hit;post(()->pick.picked(h));}});requestRender();performClick();}getParent().requestDisallowInterceptTouchEvent(false);return true;case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;}return true;}
 @Override public boolean performClick(){super.performClick();return true;}
}
